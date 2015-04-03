package nl.idgis.publisher.folder;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.folder.messages.FileChunk;
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
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof FileChunk) {
			handleFileChunk((FileChunk)msg);			
		} else if(msg instanceof End) {
			handleEnd();			
		} else {
			unhandled(msg);
		}
	}

	private void handleEnd() {		
		getSender().tell(new Ack(), getSelf());
		getContext().stop(getSelf());
	}

	private void handleFileChunk(FileChunk msg) {
		log.debug("file chunk received");
		
		byte[] content = msg.getContent();
		
		channel.write(ByteBuffer.wrap(content), position, getSender(), new CompletionHandler<Integer, ActorRef>() {

			@Override
			public void completed(Integer result, ActorRef sender) {
				sender.tell(new Ack(), getSelf());				
			}

			@Override
			public void failed(Throwable exc, ActorRef sender) {
				sender.tell(new Failure(exc), getSelf());
				getSelf().tell(PoisonPill.getInstance(), getSelf());				
			}
		});
		
		position += content.length;
	}

}
