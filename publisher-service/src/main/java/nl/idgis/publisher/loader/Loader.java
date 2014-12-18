package nl.idgis.publisher.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.harvester.messages.GetDataSource;
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
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final ActorRef geometryDatabase, database, harvester;
	
	private Map<ImportJobInfo, ActorRef> sessions;
	
	private Map<ActorRef, Progress> progress;
	
	private Set<String> busyDataSources;

	public Loader(ActorRef geometryDatabase, ActorRef database, ActorRef harvester) {
		this.geometryDatabase = geometryDatabase;
		this.database = database;
		this.harvester = harvester;
	}
	
	public static Props props(ActorRef geometryDatabase, ActorRef database, ActorRef harvester) {
		return Props.create(Loader.class, geometryDatabase, database, harvester);
	}
	
	@Override
	public void preStart() throws Exception {
		sessions = new HashMap<>();
		busyDataSources = new HashSet<>();
		progress = new HashMap<>();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ImportJobInfo) {
			handleImportJob((ImportJobInfo)msg);
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
		} else {
			unhandled(msg);
		}
	}

	private void handleProgress(Progress msg) {
		progress.put(getSender(), msg);
	}

	private void handleGetDataSource(GetDataSource msg) {
		if(busyDataSources.contains(msg.getDataSourceId())) {
			getSender().tell(new Busy(), getSelf());
		} else {
			harvester.forward(msg, getContext());
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
			
			progress.remove(sessions.remove(importJob));
			busyDataSources.remove(importJob.getDataSourceId());
			
			getSender().tell(new Ack(), getSelf());
		} else {
			log.error("unknown import job: " + importJob + " finished");
		}
	}

	private void handleSessionStarted(SessionStarted msg) {
		log.debug("data import session started: " + msg);
		
		
		ImportJobInfo job = msg.getImportJob();
		busyDataSources.add(job.getDataSourceId());
		sessions.put(job, msg.getSession());
		
		getSender().tell(new Ack(), getSelf());
	}

	private void handleImportJob(final ImportJobInfo importJob) {
		log.debug("data import requested: " + importJob);
		
		ActorRef initiator = getContext().actorOf(
				LoaderSessionInitiator.props(importJob, getSender(), database, geometryDatabase),
				nameGenerator.getName(LoaderSessionInitiator.class));
		
		database.tell(new GetDatasetStatus(importJob.getDatasetId()), initiator);
	}
	
}
