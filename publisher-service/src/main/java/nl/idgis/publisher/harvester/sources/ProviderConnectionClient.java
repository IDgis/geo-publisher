package nl.idgis.publisher.harvester.sources;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;

import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.protocol.messages.SetPersistent;

public class ProviderConnectionClient extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String harvesterName;
	
	private final ActorRef harvester, admin;
			
	public ProviderConnectionClient(String harvesterName, ActorRef harvester, ActorRef admin) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
		this.admin = admin;
	}
	
	public static Props props(String harvesterName, ActorRef harvester, ActorRef admin) {
		return Props.create(ProviderConnectionClient.class, harvesterName, harvester, admin);
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().actorOf(ProviderAdminClient.props(admin), "admin");
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
		
		ActorRef provider = getSender();
		
		provider.tell(new SetPersistent(), getSelf()); // prevent message packager termination
		provider.tell(new Hello(harvesterName), getSelf());
		
		ActorRef dataSource = getContext().actorOf(ProviderDataSource.props(provider), "data-source-" + msg.getName());		
		harvester.tell(new DataSourceConnected(msg.getName()), dataSource);
	}
}
