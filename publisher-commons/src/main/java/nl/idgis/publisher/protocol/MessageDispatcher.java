package nl.idgis.publisher.protocol;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
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
		if(msg instanceof Unreachable) {
			messagePackagerProvider.tell(msg, getSelf());
		} else if(msg instanceof Message) {			
			final ActorRef sender = getSender();
			final Message message = (Message)msg;
			
			final String targetName = message.getTargetName();
			
			final ActorPath targetPath = container.path().descendant(Arrays.asList(targetName.split("/")));
			final ActorSelection actorSelection = getContext().actorSelection(targetPath);			
			
			final ExecutionContextExecutor dispatcher = getContext().system().dispatcher();
			
			final Future<ActorRef> actorRefFuture = actorSelection.resolveOne(Duration.create(1000, TimeUnit.MILLISECONDS));
			actorRefFuture.onFailure(new OnFailure() {

				@Override
				public void onFailure(Throwable t) throws Throwable {
					log.warning("actorRef lookup failed"); 
					
					sender.tell(new Unreachable(targetName, t), sender);
				}
				
			}, dispatcher);
			
			if(message instanceof Envelope) {
				actorRefFuture.onSuccess(new OnSuccess<ActorRef>() {

					@Override
					public void onSuccess(final ActorRef actorRef) throws Throwable {
						log.warning("actorRef lookup succeeded");
						
						final Envelope envelope = (Envelope)message;
						final String sourceName = envelope.getSourceName();
					
						log.debug("target: " + actorSelection);
						
						if(sourceName == null) {
							log.debug("dispatched without sender");
							actorSelection.tell(envelope.getContent(), ActorRef.noSender());
						} else {
							log.debug("requesting packager for source: " + sourceName);
							
							GetMessagePackager request = new GetMessagePackager(envelope.getSourceName());
							Future<Object> packager = Patterns.ask(messagePackagerProvider, request, 10);
							
							packager.onSuccess(new OnSuccess<Object>() {
			
								@Override
								public void onSuccess(Object msg) throws Throwable {
									ActorRef packager = (ActorRef)msg;						
									actorRef.tell(envelope.getContent(), packager);
									
									log.debug("dispatched");
								}
								
							}, dispatcher);
						}
					}
				}, dispatcher);
			}
		} else {
			unhandled(msg);
		}
	}
}
