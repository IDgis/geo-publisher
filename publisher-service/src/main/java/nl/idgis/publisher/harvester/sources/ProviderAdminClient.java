package nl.idgis.publisher.harvester.sources;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.provider.protocol.GetActorTree;
import nl.idgis.publisher.tree.Tree;

public class ProviderAdminClient extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef admin;
	
	public ProviderAdminClient(ActorRef admin) {
		this.admin = admin;
	}
	
	public static Props props(ActorRef admin) {
		return Props.create(ProviderAdminClient.class, admin);
	}
	
	@Override
	public void preStart() throws Exception {
		if(log.isDebugEnabled()) {
			log.debug("scheduling get actor tree request");
			
			getContext().system().scheduler().schedule(
					Duration.Zero(), 
					Duration.create(5, TimeUnit.SECONDS), 
					admin, 
					new GetActorTree(), 
					getContext().dispatcher(), 
					getSelf());
		}
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Tree) {
			log.debug("remote actors: " + msg.toString());
		} else {
			unhandled(msg);
		}
	}

}
