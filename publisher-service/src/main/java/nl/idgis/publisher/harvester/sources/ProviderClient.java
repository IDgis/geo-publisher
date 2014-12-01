package nl.idgis.publisher.harvester.sources;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;

import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.protocol.messages.Hello;

public class ProviderClient extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String harvesterName;
	private final ActorRef harvester;
		
	public ProviderClient(String harvesterName, ActorRef harvester) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
	}
	
	public static Props props(String harvesterName, ActorRef harvester) {
		return Props.create(ProviderClient.class, harvesterName, harvester);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			handleHello((Hello)msg);
		} else if(msg instanceof ConnectionClosed) {
			handleConnectionClosed();
		} else {
			unhandled(msg);
		}
	}

	private void handleConnectionClosed() {
		log.debug("disconnected");
		getContext().stop(getSelf());
	}

	private void handleHello(Hello msg) {
		log.debug(msg.toString());
		
		getSender().tell(new Hello(harvesterName), getSelf());
		
		ActorRef dataSource = getContext().actorOf(ProviderDataSource.props(getSender()));		
		harvester.tell(new DataSourceConnected(msg.getName()), dataSource);
	}
}
