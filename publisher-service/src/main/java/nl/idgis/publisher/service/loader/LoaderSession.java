package nl.idgis.publisher.service.loader;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.stream.messages.End;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class LoaderSession extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	
	public LoaderSession(ActorRef database) {
		this.database = database;
	}
	
	public static Props props(ActorRef database) {
		return Props.create(LoaderSession.class, database);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Record) {
			log.debug("record received: " + msg);
			
		} else if(msg instanceof Failure) {
			log.error("import failed: " + ((Failure) msg).getCause());
			
			getContext().unbecome();
		} else if(msg instanceof End) {
			log.info("import completed");
			
			getContext().unbecome();
		}
	}
}
