package nl.idgis.publisher.harvester;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.harvester.messages.GetActiveDataSources;
import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.messages.RetryHarvest;
import nl.idgis.publisher.harvester.server.Server;
import nl.idgis.publisher.harvester.sources.messages.ListDatasets;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.HarvestJobInfo;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.ConfigUtils;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.typesafe.config.Config;

import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QDataSource.dataSource;

public class Harvester extends UntypedActor {
	
	private final Config config;
	
	private final ActorRef database, datasetManager;
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private BiMap<String, ActorRef> dataSources;
	
	private BiMap<HarvestJobInfo, ActorRef> sessions;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	private static class StartHarvesting {
		
		private final ActorRef jobContext;
		
		private final HarvestJobInfo jobInfo;
		
		private final Set<String> datasetIds;
		
		StartHarvesting(ActorRef jobContext, HarvestJobInfo jobInfo, Set<String> datasetIds) {
			this.jobContext = jobContext;
			this.jobInfo = jobInfo;
			this.datasetIds = datasetIds;
		}
		
		public ActorRef getJobContext() {
			return this.jobContext;
		}

		public HarvestJobInfo getJobInfo() {
			return jobInfo;
		}

		public Set<String> getDatasetIds() {
			return datasetIds;
		}
	}

	public Harvester(ActorRef database, ActorRef datasetManager, Config config) {
		this.database = database;
		this.datasetManager = datasetManager;
		this.config = config;
	}
	
	public static Props props(ActorRef database, ActorRef datasetManager, Config config) {
		return Props.create(Harvester.class, database, datasetManager, config);
	}

	@Override
	public void preStart() {
		final String name = config.getString("name");		
		final int port = config.getInt("port");
		
		getContext().actorOf(Server.props(name, getSelf(), port, config), "server");
		
		dataSources = HashBiMap.create();
		
		sessions = HashBiMap.create();
		
		f = new FutureUtils(getContext());
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("message: " + msg);
		
		if(msg instanceof DataSourceConnected) {
			handleDataSourceConnected((DataSourceConnected)msg);
		} else if (msg instanceof Terminated) {
			handleTerminated((Terminated)msg);
		} else if (msg instanceof HarvestJobInfo) {
			handleHarvestJob((HarvestJobInfo)msg);			
		} else if(msg instanceof GetActiveDataSources) {
			handleGetActiveDataSources();
		} else if(msg instanceof GetDataSource) {
			handleGetDataSource((GetDataSource)msg);
		} else if(msg instanceof GetActiveJobs) {
			handleGetActiveJobs();
		} else if(msg instanceof StartHarvesting) {
			handleStartHarvesting((StartHarvesting)msg);
		} else if(msg instanceof RetryHarvest) {
			handleRetryHarvest((RetryHarvest)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleRetryHarvest(RetryHarvest msg) {
		HarvestJobInfo harvestJob = msg.getJobInfo();
		String dataSourceId = harvestJob.getDataSourceId();
		
		log.debug("retrying harvest for dataSource: {}", dataSourceId);
		
		if(dataSources.containsKey(dataSourceId)) {
			if(sessions.containsKey(harvestJob)) {
				ActorRef session = sessions.get(harvestJob);
				dataSources.get(dataSourceId).tell(new ListDatasets(), session);
			} else {
				log.error("retry requested but not harvesting");
			}
		} else {
			log.error("retry requested but dataSource is not available");
		}
	}

	private void handleStartHarvesting(StartHarvesting msg) {
		HarvestJobInfo harvestJob = msg.getJobInfo();
		
		boolean includeConfidential;
		if(config.hasPath("includeConfidential")) {
			includeConfidential = config.getBoolean("includeConfidential");
		} else {
			includeConfidential = true;
		}
		
		ActorRef session = getContext().actorOf(
				HarvestSession.props(msg.getJobContext(), datasetManager, 
					harvestJob, msg.getDatasetIds(), includeConfidential), 
				nameGenerator.getName(HarvestSession.class));
		
		getContext().watch(session);
		sessions.put(harvestJob, session);
		
		dataSources.get(harvestJob.getDataSourceId()).tell(new ListDatasets(), session);
	}

	private void handleGetActiveJobs() {
		ArrayList<ActiveJob> activeJobs = new ArrayList<>();
		for(HarvestJobInfo harvestJob : sessions.keySet()) {
			activeJobs.add(new ActiveJob(harvestJob));
		}
		
		getSender().tell(new ActiveJobs(activeJobs), getSelf());
	}

	private void handleGetDataSource(GetDataSource msg) {
		log.debug("dataSource requested");
		
		final String dataSourceId = msg.getDataSourceId();
		if(dataSources.containsKey(dataSourceId)) {
			getSender().tell(dataSources.get(dataSourceId), getSelf());
		} else {
			log.warning("dataSource not connected: " + dataSourceId);
			getSender().tell(new NotConnected(), getSelf());
		}
	}

	private void handleGetActiveDataSources() {
		log.debug("connected datasources requested");
		getSender().tell(dataSources.keySet(), getSelf());
	}
	
	private boolean isHarvesting(String dataSourceId) {
		for(HarvestJobInfo job : sessions.keySet()) {
			if(job.getDataSourceId().equals(dataSourceId)) {
				return true;
			}
		}
		
		return false;
	}

	private void handleHarvestJob(HarvestJobInfo harvestJob) {
		String dataSourceId = harvestJob.getDataSourceId();
		if(dataSources.containsKey(dataSourceId)) {
			if(isHarvesting(dataSourceId)) {
				log.debug("already harvesting dataSource: " + dataSourceId);
			} else {
				log.debug("Initializing harvesting for dataSource: " + dataSourceId);			
			
				ActorRef sender = getSender(), self = getSelf();
				f.ask(sender, new UpdateJobState(JobState.STARTED)).whenComplete((msg, t) -> {
					if(t != null) {
						log.error("couldn't change job state: {}", t);
						sender.tell(new Ack(), getSelf());
					} else {
						log.debug("starting harvesting for dataSource: " + harvestJob);
						
						sender.tell(new Ack(), getSelf());
						
						db.query().from(sourceDataset)
							.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
							.where(sourceDataset.deleteTime.isNull()
								.and(dataSource.identification.eq(dataSourceId)))
							.list(sourceDataset.externalIdentification).thenAccept(datasetIds ->						
								self.tell(
									new StartHarvesting(
										sender, 
										harvestJob, 
										datasetIds.list().stream()
											.collect(Collectors.toSet())), 
										
									self));
					}
				});
			}
		} else {
			getSender().tell(new Ack(), getSelf());
			
			log.debug("dataSource not connected: " + dataSourceId);
		}
	}

	private void handleTerminated(Terminated msg) {
		ActorRef actor = msg.getActor();
		
		log.debug("actor terminated: " + actor);
		
		String dataSourceName = dataSources.inverse().remove(actor);
		if(dataSourceName != null) {
			log.debug("connection lost, dataSource: " + dataSourceName);
		}
		
		HarvestJobInfo harvestJob = sessions.inverse().remove(actor);
		if(harvestJob != null) {
			log.debug("harvest job completed: " + harvestJob);			
		}
	}

	private void handleDataSourceConnected(DataSourceConnected msg) {
		log.debug("dataSource connected: {}", msg);
		
		String dataSourceId = msg.getDataSourceId();
		ActorRef dataSource = msg.getDataSource();
		
		getContext().watch(dataSource);
		dataSources.put(dataSourceId, dataSource);
		
		getSender().tell(new Ack(), getSelf());
	}
}
