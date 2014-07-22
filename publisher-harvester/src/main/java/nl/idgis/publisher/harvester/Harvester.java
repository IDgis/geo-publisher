package nl.idgis.publisher.harvester;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import nl.idgis.publisher.harvester.messages.DataSources;
import nl.idgis.publisher.harvester.messages.GetDataSources;
import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.harvester.server.Server;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetList;
import nl.idgis.publisher.utils.ConfigUtils;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.typesafe.config.Config;

public class Harvester extends UntypedActor {
	
	private final Config config;
	private final ActorRef database;
	private final LoggingAdapter log;	
	
	private BiMap<String, ActorRef> dataSources;

	public Harvester(ActorRef database, Config config) {
		this.database = database;
		this.config = config;
		
		log = Logging.getLogger(getContext().system(), this);
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
		
		FiniteDuration interval = Duration.create(10, TimeUnit.SECONDS);
		getContext().system().scheduler().schedule(Duration.Zero(), interval, getSelf(), new Harvest(), getContext().dispatcher(), getSelf());
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
				for(ActorRef dataSource : dataSources.values()) {
					dataSource.tell(new GetDatasetList(), getSelf());
				}
			} else {
				if(dataSources.containsKey(dataSourceId)) {
					log.debug("Initializing harvesting for dataSource: " + dataSourceId);
				} else {
					dataSources.get(dataSourceId).tell(new GetDatasetList(), getSelf());
				}
			}
		} else if(msg instanceof GetDataSources) {
			log.debug("connected datasources requested");
			getSender().tell(new DataSources(dataSources.keySet()), getSelf());
		} else {
			unhandled(msg);
		}
	}
}
