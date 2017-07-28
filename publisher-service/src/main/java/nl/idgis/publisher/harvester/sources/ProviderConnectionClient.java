package nl.idgis.publisher.harvester.sources;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;

import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.protocol.messages.SetPersistent;
import nl.idgis.publisher.utils.FutureUtils;

public class ProviderConnectionClient extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String harvesterName;
	
	private final ActorRef harvester, admin;
	
	private final Config harvesterConfig;
	
	private FutureUtils f;
			
	public ProviderConnectionClient(String harvesterName, ActorRef harvester, ActorRef admin, Config harvesterConfig) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
		this.admin = admin;
		this.harvesterConfig = harvesterConfig;
	}
	
	public static Props props(String harvesterName, ActorRef harvester, ActorRef admin, Config harvesterConfig) {
		return Props.create(ProviderConnectionClient.class, harvesterName, harvester, admin, harvesterConfig);
	}
	
	@Override
	public void preStart() throws Exception {
		f = new FutureUtils(getContext());
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
		
		ActorRef sender = getSender(), self = getSelf();		
		sender.tell(new SetPersistent(), getSelf()); // prevent message packager termination
		
		ActorRef dataSource = getContext().actorOf(ProviderDataSource.props(sender, harvesterConfig), "data-source-" + msg.getName());
		f.ask(harvester, new DataSourceConnected(msg.getName(), dataSource), Ack.class).whenComplete((ack, t) -> {
			if(t != null) {
				log.error("failed to register data source: {}", t);				
				sender.tell(new Failure(t), self);
			} else {
				sender.tell(new Hello(harvesterName), self);
			}
		});
	}
}
