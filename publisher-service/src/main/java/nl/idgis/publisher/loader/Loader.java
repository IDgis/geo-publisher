package nl.idgis.publisher.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.job.manager.messages.RasterImportJobInfo;
import nl.idgis.publisher.job.manager.messages.VectorImportJobInfo;
import nl.idgis.publisher.job.manager.messages.RemoveJobInfo;
import nl.idgis.publisher.loader.messages.Busy;
import nl.idgis.publisher.loader.messages.SessionFinished;
import nl.idgis.publisher.loader.messages.SessionStarted;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class Loader extends UntypedActor {
	
	private static final int MAXIMUM_SESSIONS_PER_DATASOURCE = 1;

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final ActorRef database, rasterFolder, harvester, datasetManager;
	
	private BiMap<ImportJobInfo, ActorRef> sessions;
	
	private Map<ImportJobInfo, ActorRef> jobContexts;
	
	private Map<ActorRef, Progress> progress;
	
	private Map<String, Integer> busyDataSources;

	public Loader(ActorRef database, ActorRef rasterFolder, ActorRef harvester, ActorRef datasetManager) {		
		this.database = database;
		this.rasterFolder = rasterFolder;
		this.harvester = harvester;
		this.datasetManager = datasetManager;
	}
	
	public static Props props(ActorRef database, ActorRef rasterFolder, ActorRef harvester, ActorRef datasetManager) {
		return Props.create(Loader.class, database, rasterFolder, harvester, datasetManager);
	}
	
	@Override
	public void preStart() throws Exception {
		sessions = HashBiMap.create();
		busyDataSources = new HashMap<>();
		progress = new HashMap<>();
		jobContexts = new HashMap<>();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ImportJobInfo) {
			handleImportJob((ImportJobInfo)msg);
		} else if(msg instanceof RemoveJobInfo) {
			handleRemoveJob((RemoveJobInfo)msg);
		} else if(msg instanceof SessionStarted) {
			handleSessionStarted((SessionStarted)msg);
		} else if(msg instanceof SessionFinished) {
			handleSessionFinished((SessionFinished)msg);
		} else if(msg instanceof GetActiveJobs) {
			handleGetActiveJobs((GetActiveJobs)msg);
		} else if(msg instanceof GetDataSource) {
			handleGetDataSource((GetDataSource)msg);
		} else if(msg instanceof Progress) {
			handleProgress((Progress)msg);
		} else if(msg instanceof Terminated) {
			handleTerminated((Terminated)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private void handleTerminated(Terminated msg) {
		ActorRef session = msg.getActor();
		
		log.debug("terminated: {}", session);
		
		ImportJobInfo importJob = sessions.inverse().get(session);
		if(importJob == null) {
			log.error("session unknown");
		} else {
			log.debug("aborting job");
			
			ActorRef jobContext = jobContexts.get(importJob);
			if(jobContext == null) {
				log.error("job context not found");
			} else {
				jobContext.tell(new UpdateJobState(JobState.ABORTED), getSelf());
			}
			
			log.debug("finishing session");
			getSelf().tell(new SessionFinished(importJob), getSelf());
		}
	}

	private void handleProgress(Progress msg) {
		progress.put(getSender(), msg);
	}

	private void handleGetDataSource(GetDataSource msg) {
		log.debug("get data source");
		
		if(isBusy(msg.getDataSourceId())) {
			getSender().tell(new Busy(), getSelf());
		} else {
			harvester.forward(msg, getContext());
		}
	}

	private boolean isBusy(String dataSourceId) {
		if(busyDataSources.containsKey(dataSourceId)) {			
			if(busyDataSources.get(dataSourceId) < MAXIMUM_SESSIONS_PER_DATASOURCE) {
				log.debug("more sessions are allowed for data source");
				return false;
			} else {
				log.debug("data source is busy");				
				return true;
			}
		} else {
			log.debug("data source is not busy");
			return false;
		}
	}

	private void handleGetActiveJobs(GetActiveJobs msg) {
		List<ActiveJob> activeJobs = new ArrayList<>();
		for(Map.Entry<ImportJobInfo, ActorRef> session : sessions.entrySet()) {
			JobInfo jobInfo = session.getKey();
			
			ActorRef actor = session.getValue();
			activeJobs.add(new ActiveJob(jobInfo, progress.get(actor)));
		}
		
		getSender().tell(new ActiveJobs(activeJobs), getSelf());
	}

	private void handleSessionFinished(SessionFinished msg) {
		ImportJobInfo importJob = msg.getImportJob();
		
		if(sessions.containsKey(importJob)) {
			log.debug("import job finished: " + importJob);
			
			ActorRef session = sessions.remove(importJob);
			getContext().unwatch(session);
			
			String dataSourceId = importJob.getDataSourceId();
			if(busyDataSources.containsKey(dataSourceId)) {
				int currentSessionCount = busyDataSources.get(dataSourceId);
				if(currentSessionCount == 1) {
					log.debug("last session for data source finished");					
					busyDataSources.remove(dataSourceId);
				} else {
					log.debug("session for data source finished");
					busyDataSources.put(dataSourceId, currentSessionCount - 1);
				}				
			} else {
				log.error("data source missing from busy data sources");
			}
			
			jobContexts.remove(importJob);
			
			getSender().tell(new Ack(), getSelf());
		} else {
			log.error("unknown import job: " + importJob + " finished");
		}
	}

	private void handleSessionStarted(SessionStarted msg) {
		log.debug("data import session started: " + msg);		
		
		ImportJobInfo job = msg.getImportJob();

		String dataSourceId = job.getDataSourceId();
		if(busyDataSources.containsKey(dataSourceId)) {
			log.debug("additional session for data source started");
			int currentSessionCount = busyDataSources.get(dataSourceId);
			busyDataSources.put(dataSourceId, currentSessionCount + 1);
		} else {
			log.debug("first session for data source started");
			busyDataSources.put(dataSourceId, 1);
		}
		
		ActorRef session = msg.getSession();
		getContext().watch(session);
		sessions.put(job, session);
		
		getSender().tell(new Ack(), getSelf());
	}
	
	private void handleImportJob(ImportJobInfo importJob) {
		log.debug("starting import job: {}", importJob);
		
		log.debug("data import requested: " + importJob);
		
		ActorRef initiator;
		ActorRef jobContext = getSender();
		if(importJob instanceof VectorImportJobInfo) {
			initiator = getContext().actorOf(
				VectorLoaderSessionInitiator.props((VectorImportJobInfo)importJob, jobContext, database, datasetManager),
				nameGenerator.getName(VectorLoaderSessionInitiator.class));
		} else if(importJob instanceof RasterImportJobInfo) {
			initiator = getContext().actorOf(
				RasterLoaderSessionInitiator.props((RasterImportJobInfo)importJob, jobContext, rasterFolder),
				nameGenerator.getName(RasterLoaderSessionInitiator.class));
		} else {
			unhandled(importJob);
			return;
		}
		
		jobContexts.put(importJob, jobContext);		
		getSelf().tell(new GetDataSource(importJob.getDataSourceId()), initiator);
	}
	
	private void handleRemoveJob(final RemoveJobInfo removeJob) {
		log.debug("data remove requested: " + removeJob);
		
		// TODO: actually remove dataset
	}
	
}
