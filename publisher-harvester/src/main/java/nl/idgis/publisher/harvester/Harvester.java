package nl.idgis.publisher.harvester;

import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.domain.job.GenericJobLogType;
import nl.idgis.publisher.domain.job.HarvestJobLog;
import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.harvester.messages.GetActiveDataSources;
import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.Harvest;
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
			log.debug("dispatching metadata parsing request");
			
			metadataDocumentFactory.tell(msg, getSender());
		} else if(msg instanceof DataSourceConnected) {
			String dataSourceId = ((DataSourceConnected) msg).getDataSourceId();
			log.debug("DataSource connected: " + dataSourceId);
			
			getContext().watch(getSender());
			dataSources.put(dataSourceId, getSender());
		} else if (msg instanceof Terminated) {
			String dataSourceName = dataSources.inverse().remove(((Terminated) msg).getActor());
			if(dataSourceName != null) {
				log.debug("Connection lost, dataSource: " + dataSourceName);
			}
		} else if (msg instanceof Harvest) {
			String dataSourceId = ((Harvest) msg).getDataSourceId();
			
			if(dataSourceId == null) {
				log.debug("Initializing harvesting for all dataSources");
				for(String currentDataSourceId : dataSources.keySet()) {
					startHarvesting(currentDataSourceId);
				}
			} else {
				if(dataSources.containsKey(dataSourceId)) {
					log.debug("Initializing harvesting for dataSource: " + dataSourceId);
					
					startHarvesting(dataSourceId);
				} else {
					log.debug("dataSource not connected: " + dataSourceId);
				}
			}
		} else if(msg instanceof GetActiveDataSources) {
			log.debug("connected datasources requested");
			getSender().tell(dataSources.keySet(), getSelf());
		} else if(msg instanceof GetDataSource) {
			log.debug("dataSource requested");
			
			final String dataSourceId = ((GetDataSource) msg).getDataSourceId();
			if(dataSources.containsKey(dataSourceId)) {
				getSender().tell(dataSources.get(dataSourceId), getSelf());
			} else {
				log.warning("dataSource not connected: " + dataSourceId);
				getSender().tell(new NotConnected(), getSelf());
			}		 
		} else {
			unhandled(msg);
		}
	}

	private void startHarvesting(final String dataSourceId) {
		HarvestJobLog logLine = new HarvestJobLog(GenericJobLogType.STARTED, dataSourceId);
		Patterns.ask(database, new StoreLog(logLine), 150000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("starting harvesting for dataSource: " + dataSourceId);
					
					ActorRef session = getContext().actorOf(HarvestSession.props(database, dataSourceId));
					dataSources.get(dataSourceId).tell(new GetDatasets(), session);
				}
			}, getContext().dispatcher());
	}
}
