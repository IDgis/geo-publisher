package nl.idgis.publisher.harvester;

import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.domain.log.GenericEvent;
import nl.idgis.publisher.domain.log.HarvestLogLine;
import nl.idgis.publisher.harvester.messages.GetActiveDataSources;
import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.harvester.messages.RequestDataset;
import nl.idgis.publisher.harvester.server.Server;
import nl.idgis.publisher.harvester.sources.messages.GetDataset;
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
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof DataSourceConnected) {
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
		} else if(msg instanceof RequestDataset) {
			RequestDataset requestDataset = (RequestDataset)msg;
			
			log.debug("dataset requested");
			String dataSourceId = requestDataset.getDataSourceId();
			if(dataSources.containsKey(dataSourceId)) {
				log.debug("requesting dataSource to send data");
				
				ActorRef dataSource = dataSources.get(dataSourceId);
				dataSource.tell(new GetDataset(
						requestDataset.getSourceDatasetId()), getSender());
			} else {
				log.warning("dataSource not connected: " + dataSourceId);
			}		 
		} else {
			unhandled(msg);
		}
	}

	private void startHarvesting(final String dataSourceId) {
		HarvestLogLine logLine = new HarvestLogLine(GenericEvent.STARTED, dataSourceId);
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
