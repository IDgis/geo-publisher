package nl.idgis.publisher.harvester;

import nl.idgis.publisher.database.messages.HarvestJob;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.harvester.messages.GetActiveDataSources;
import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.harvester.metadata.messages.ParseMetadataDocument;
import nl.idgis.publisher.harvester.server.Server;
import nl.idgis.publisher.harvester.sources.messages.GetDatasets;
import nl.idgis.publisher.utils.ConfigUtils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.typesafe.config.Config;

public class Harvester extends UntypedActor {
	
	private final Config config;
	private final ActorRef database;
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);	
	
	private BiMap<String, ActorRef> dataSources;
	private ActorRef metadataDocumentFactory;

	public Harvester(ActorRef database, Config config) {
		this.database = database;
		this.config = config;
	}
	
	public static Props props(ActorRef database, Config config) {
		return Props.create(Harvester.class, database, config);
	}

	@Override
	public void preStart() {
		final String name = config.getString("name");		
		final int port = config.getInt("port");
		
		final Config sslConfig = ConfigUtils.getOptionalConfig(config, "ssl");
		
		getContext().actorOf(Server.props(name, getSelf(), port, sslConfig), "server");
		
		dataSources = HashBiMap.create();
		
		metadataDocumentFactory = getContext().actorOf(MetadataDocumentFactory.props(), "metadataDocumentFactory");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("message: " + msg);
		
		if(msg instanceof ParseMetadataDocument) {
			handleParseMetadataDocument((ParseMetadataDocument)msg);
		} else if(msg instanceof DataSourceConnected) {
			handleDataSourceConnected((DataSourceConnected)msg);
		} else if (msg instanceof Terminated) {
			handleTerminated((Terminated)msg);
		} else if (msg instanceof HarvestJob) {
			handleHarvestJob((HarvestJob)msg);			
		} else if(msg instanceof GetActiveDataSources) {
			handleGetActiveDataSources();
		} else if(msg instanceof GetDataSource) {
			handleGetDataSource((GetDataSource)msg);		 
		} else {
			unhandled(msg);
		}
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

	private void handleHarvestJob(HarvestJob harvestJob) {
		String dataSourceId = harvestJob.getDataSourceId();
		if(dataSources.containsKey(dataSourceId)) {
			log.debug("Initializing harvesting for dataSource: " + dataSourceId);
			
			startHarvesting(harvestJob);
		} else {
			log.debug("dataSource not connected: " + dataSourceId);
		}
	}

	private void handleTerminated(Terminated msg) {
		String dataSourceName = dataSources.inverse().remove(msg.getActor());
		if(dataSourceName != null) {
			log.debug("Connection lost, dataSource: " + dataSourceName);
		}
	}

	private void handleDataSourceConnected(DataSourceConnected msg) {
		String dataSourceId = msg.getDataSourceId();
		log.debug("DataSource connected: " + dataSourceId);
		
		getContext().watch(getSender());
		dataSources.put(dataSourceId, getSender());
	}

	private void handleParseMetadataDocument(ParseMetadataDocument msg) {
		log.debug("dispatching metadata parsing request");
		
		metadataDocumentFactory.tell(msg, getSender());
	}

	private void startHarvesting(final HarvestJob harvestJob) {		
		Patterns.ask(database, new UpdateJobState(harvestJob, JobState.STARTED), 150000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("starting harvesting for dataSource: " + harvestJob);
					
					ActorRef session = getContext().actorOf(HarvestSession.props(database, harvestJob));
					dataSources.get(harvestJob.getDataSourceId()).tell(new GetDatasets(), session);
				}
			}, getContext().dispatcher());
	}
}
