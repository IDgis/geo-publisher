package nl.idgis.publisher.provider;

import nl.idgis.publisher.monitor.messages.GetTree;
import nl.idgis.publisher.provider.protocol.GetActorTree;
import nl.idgis.publisher.provider.protocol.Restart;
import nl.idgis.publisher.tree.Tree;
import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Kill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Admin extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorSelection app, monitor;
	
	private FutureUtils f;
	
	public static Props props() {
		return Props.create(Admin.class);
	}
	
	@Override
	public void preStart() throws Exception {
		app = getContext().actorSelection("/user/app");
		monitor = getContext().actorSelection("/user/monitor");
		
		f = new FutureUtils(getContext());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Restart) {
			log.info("killing application");
			
			app.tell(Kill.getInstance(), getSelf());
		} else if(msg instanceof GetActorTree) {
			log.info("actor tree requested");
			
			// we have to manually forward the message
			// as the monitor actor isn't allowed to talk
			// to the publisher service directly.
			ActorRef self = getSelf(), sender = getSender();
			f.ask(monitor, new GetTree(), Tree.class).thenAccept(tree -> {
				sender.tell(tree, self);
			});
		} else {
			unhandled(msg);
		}
	}

}
