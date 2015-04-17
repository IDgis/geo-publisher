package nl.idgis.publisher.folder;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.End;

public class ChannelReceiver extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final AsynchronousFileChannel channel;
	
	private long position;
	
	public ChannelReceiver(AsynchronousFileChannel channel) {
		this.channel = channel;
	}
	
	public static Props props(AsynchronousFileChannel channel) {
		return Props.create(ChannelReceiver.class, channel);
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.apply(30, TimeUnit.SECONDS));
	}
	
	@Override
	public final void postStop() throws Exception {
		channel.close();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof byte[]) {
			handleFileChunk((byte[])msg);			
		} else if(msg instanceof End) {
			handleEnd();			
		} else if(msg instanceof ReceiveTimeout) {
			log.error("timeout");
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}

	private void handleEnd() {		
		getSender().tell(new Ack(), getSelf());
		getContext().stop(getSelf());
	}

	private void handleFileChunk(byte[] content) {
		log.debug("file chunk received");
		
		channel.write(ByteBuffer.wrap(content), position, getSender(), new CompletionHandler<Integer, ActorRef>() {

			@Override
			public void completed(Integer result, ActorRef sender) {
				log.debug("ack: {} {}", result, content.length);
				sender.tell(new Ack(), getSelf());				
			}

			@Override
			public void failed(Throwable exc, ActorRef sender) {
				log.error("failure: {}", exc);
				sender.tell(new Failure(exc), getSelf());
				getSelf().tell(PoisonPill.getInstance(), getSelf());				
			}
		});
		
		position += content.length;
		
		log.debug("position: {}", position);
	}

}
