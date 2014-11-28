package nl.idgis.publisher.provider;

import java.io.File;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.provider.database.Database;
import nl.idgis.publisher.provider.metadata.Metadata;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.FetchTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.metadata.GetAllMetadata;
import nl.idgis.publisher.provider.protocol.metadata.GetMetadata;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Provider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config config;
	
	private final String name;
	
	private ActorRef database, metadata;
	
	public Provider(Config config, String name) {
		this.config = config;
		this.name = name;
	}
	
	public static Props props(Config config, String name) {
		return Props.create(Provider.class, config, name);
	}
	
	@Override
	public void preStart() {
		Config databaseConfig = config.getConfig("database");		
		
		database = getContext().actorOf(Database.props(databaseConfig, name), "database");

		metadata = getContext().actorOf(Metadata.props(
			new File(config.getString("metadata.folder"))), "metadata");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.debug(msg.toString());
		} else if(msg instanceof GetAllMetadata) {
			metadata.forward(msg, getContext());
		} else if(msg instanceof GetMetadata) {
			metadata.forward(msg, getContext());
		} else if(msg instanceof DescribeTable) {
			database.forward(msg, getContext());
		} else if(msg instanceof FetchTable) {
			database.forward(msg, getContext());
		} else if(msg instanceof PerformCount) {
			database.forward(msg, getContext());
		} else {
			unhandled(msg);
		}
	}

}
