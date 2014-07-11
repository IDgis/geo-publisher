package nl.idgis.publisher.protocol;

import java.util.Arrays;

import nl.idgis.publisher.utils.OnReceive;
import scala.concurrent.Future;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class MessageDispatcher extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef messagePackagerProvider, container;
	
	public MessageDispatcher(ActorRef messagePackagerProvider, ActorRef container) {
		this.messagePackagerProvider = messagePackagerProvider;
		this.container = container;
	}
	
	public static Props props(ActorRef messagePackagerProvider, ActorRef container) {
		return Props.create(MessageDispatcher.class, messagePackagerProvider, container);
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof Message) {
			final Message message = (Message)msg;
			final String sourceName = message.getSourceName();
			
			ActorPath targetPath = container.path().descendant(Arrays.asList(message.getTargetName().split("/")));
			final ActorSelection actorSelection = getContext().actorSelection(targetPath);
		
			log.debug("target: " + actorSelection);
			
			if(sourceName == null) {
				log.debug("dispatched without sender");
				actorSelection.tell(message.getContent(), ActorRef.noSender());
			} else {
				log.debug("requesting packager for source: " + sourceName);
				
				GetMessagePackager request = new GetMessagePackager(message.getSourceName());
				Future<Object> packager = Patterns.ask(messagePackagerProvider, request, 10);
			
				packager.onComplete(new OnReceive<ActorRef>(log, ActorRef.class) {
	
					@Override
					protected void onReceive(ActorRef packager) {
						log.debug("dispatched");
						actorSelection.tell(message.getContent(), packager);
					}
				}, getContext().system().dispatcher());
			}
		} else {
			unhandled(msg);
		}
	}
}
