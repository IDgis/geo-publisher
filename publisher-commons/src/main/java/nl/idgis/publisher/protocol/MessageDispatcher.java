package nl.idgis.publisher.protocol;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Destroyed;
import nl.idgis.publisher.protocol.messages.Envelope;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;
import nl.idgis.publisher.protocol.messages.Message;
import nl.idgis.publisher.protocol.messages.StopPackager;
import nl.idgis.publisher.protocol.messages.Unreachable;
import scala.collection.Iterator;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.Terminated;
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
		if(msg instanceof Message) {
			final Message message = (Message)msg;
			
			if(message instanceof Unreachable
				|| message instanceof Destroyed) {
				
				log.debug("stopping packager");
				messagePackagerProvider.tell(new StopPackager(message.getTargetName()), getSelf());
			} else {
				final ActorRef sender = getSender();
				
				final String targetName = message.getTargetName();
				
				final ActorPath targetPath = container.path().descendant(Arrays.asList(targetName.split("/")));
				final ActorSelection actorSelection = getContext().actorSelection(targetPath);			
				
				final ExecutionContextExecutor dispatcher = getContext().system().dispatcher();
				
				final Future<ActorRef> actorRefFuture = actorSelection.resolveOne(Duration.create(1000, TimeUnit.MILLISECONDS));
				actorRefFuture.onFailure(new OnFailure() {
	
					@Override
					public void onFailure(Throwable t) throws Throwable {
						log.warning("actorRef lookup failed, message: " + message); 
						
						sender.tell(new Unreachable(targetName, t.getMessage()), sender);
					}
					
				}, dispatcher);
				
				if(message instanceof Envelope) {
					actorRefFuture.onSuccess(new OnSuccess<ActorRef>() {
	
						@Override
						public void onSuccess(final ActorRef actorRef) throws Throwable {
							log.debug("actorRef lookup succeeded");
							
							getContext().watch(actorRef);
							
							final Envelope envelope = (Envelope)message;
							final String sourceName = envelope.getSourceName();
						
							log.debug("target: " + actorSelection);
							
							if(sourceName == null) {
								log.debug("dispatched without sender");
								actorSelection.tell(envelope.getContent(), ActorRef.noSender());
							} else {
								log.debug("requesting packager for source: " + sourceName);
								
								GetMessagePackager request = new GetMessagePackager(envelope.getSourceName(), false);
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
			} 
		} else if(msg instanceof Terminated) {
			ActorPath path = ((Terminated) msg).getActor().path();
			
			Iterator<String> itr = path.elements().iterator();
			for(int i = 0; i < container.path().elements().size(); i++) {
				itr.next();
			}
			
			String separator = "";
			StringBuilder sb = new StringBuilder();
			while(itr.hasNext()) {
				sb.append(separator);
				sb.append(itr.next());
				
				separator = "/";
			}
			
			String targetName = sb.toString();
			getContext().parent().tell(new Destroyed(targetName), getSelf());
		} else if(msg instanceof Ack) {
			log.debug("message acknowledged by: " + getSender());
		} else {
			unhandled(msg);
		}
	}
}
