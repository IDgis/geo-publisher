package nl.idgis.publisher.harvester.sources;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.protocol.messages.SetPersistent;
import nl.idgis.publisher.provider.protocol.EchoRequest;
import nl.idgis.publisher.provider.protocol.EchoResponse;

public class ProviderClient extends UntypedActor {
	
	private final static FiniteDuration ECHO_INTERVAL = Duration.create(30, TimeUnit.SECONDS);
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String harvesterName;
	
	private final ActorRef harvester;
	
	private ActorRef provider;
			
	public ProviderClient(String harvesterName, ActorRef harvester) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
	}
	
	public static Props props(String harvesterName, ActorRef harvester) {
		return Props.create(ProviderClient.class, harvesterName, harvester);
	} 
	
	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.apply(2, TimeUnit.MINUTES));
	}
	
	private void scheduleEchoRequest(BigInteger payload) {
		getContext().system().scheduler().scheduleOnce(
				ECHO_INTERVAL, provider, new EchoRequest(payload), 
					getContext().dispatcher(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			handleTimeout();
		} else if(msg instanceof Hello) {
			handleHello((Hello)msg);
		} else if(msg instanceof ConnectionClosed) {
			handleConnectionClosed();
		} else if(msg instanceof EchoResponse) {
			handleEchoResponse((EchoResponse)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleEchoResponse(EchoResponse msg) {
		log.debug("echo response: {}", msg);

		BigInteger payload = (BigInteger)msg.getPayload();
		scheduleEchoRequest(payload.add(BigInteger.ONE));
	}

	private void handleTimeout() {
		log.error("timeout");		
		getContext().stop(getSelf());
	}

	private void handleConnectionClosed() {
		log.debug("disconnected");
		getContext().stop(getSelf());
	}

	private void handleHello(Hello msg) {
		log.debug(msg.toString());
		
		provider = getSender();		
		scheduleEchoRequest(BigInteger.ZERO);
		
		provider.tell(new SetPersistent(), getSelf()); // prevent message packager termination
		provider.tell(new Hello(harvesterName), getSelf());
		
		ActorRef dataSource = getContext().actorOf(ProviderDataSource.props(provider), msg.getName());		
		harvester.tell(new DataSourceConnected(msg.getName()), dataSource);
	}
}
