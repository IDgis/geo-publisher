package nl.idgis.publisher.harvester.sources;

import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.harvester.sources.messages.GetDatasets;
import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.provider.protocol.metadata.GetMetadata;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;
import akka.japi.Procedure;

public class ProviderClient extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String harvesterName;
	private final ActorRef harvester, metadata, database;
		
	public ProviderClient(String harvesterName, ActorRef harvester, ActorRef metadata, ActorRef database) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
		this.metadata = metadata;
		this.database = database;
	}
	
	public static Props props(String harvesterName, ActorRef harvester, ActorRef metadata, ActorRef database) {
		return Props.create(ProviderClient.class, harvesterName, harvester, metadata, database);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.debug(msg.toString());
			
			getSender().tell(new Hello(harvesterName), getSelf());
			getContext().become(active(), false);
			harvester.tell(new DataSourceConnected(((Hello) msg).getName()), getSelf());
		} else if(msg instanceof ConnectionClosed) {
			log.debug("disconnected");
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	private Procedure<Object> active() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof GetDatasets) {
					log.debug("retrieving datasets from provider");
					
					ActorRef providerDataset = getContext().actorOf(ProviderDataset.props(harvester, database));
					metadata.tell(new GetMetadata(), providerDataset);
				} else if(msg instanceof ConnectionClosed) {
					log.debug("disconnected");
					getContext().stop(getSelf());
				} else {
					unhandled(msg);
				} 
			}
		};
	}
}
