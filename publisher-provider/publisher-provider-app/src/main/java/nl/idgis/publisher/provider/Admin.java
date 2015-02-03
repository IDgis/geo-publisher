package nl.idgis.publisher.provider;

import nl.idgis.publisher.provider.protocol.Restart;

import akka.actor.ActorSelection;
import akka.actor.Kill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Admin extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorSelection app;
	
	public static Props props() {
		return Props.create(Admin.class);
	}
	
	@Override
	public void preStart() throws Exception {
		app = getContext().actorSelection("/user/app");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Restart) {
			log.info("killing application");
			
			app.tell(Kill.getInstance(), getSelf());
		} else {
			unhandled(msg);
		}
	}

}
