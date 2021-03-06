package nl.idgis.publisher.folder;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.folder.messages.Eof;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.StreamCursor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ChannelCursor extends StreamCursor<AsynchronousFileChannel, byte[]> {
	
	private static final int CHUNK_SIZE = 102400;
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ByteBuffer buffer;
	
	private long position;
	
	private boolean eof;
	
	private CompletableFuture<byte[]> future;

	public ChannelCursor(AsynchronousFileChannel channel) {
		super(channel);
		
		buffer = ByteBuffer.allocateDirect(CHUNK_SIZE);
	}
	
	@Override
	public final void postStop() throws Exception {
		t.close();
	}
	
	protected void preStartElse() throws Exception {
		buffer.clear();
		position = 0;
		eof = false;
	}
	
	public static Props props(AsynchronousFileChannel channel) {
		return Props.create(ChannelCursor.class, channel);
	}

	@Override
	protected boolean hasNext() throws Exception {		
		return !eof;
	}
	
	@Override
	protected void onReceiveElse(Object msg) {
		if(msg instanceof byte[]) {
			byte[] chunk = (byte[])msg;
			
			future.complete(chunk);
			position += chunk.length;
		} else if(msg instanceof Eof) {
			eof = true;
		} else if(msg instanceof Failure) {
			Throwable cause = ((Failure) msg).getCause();
			log.error("failure: {}", cause);
			
			future.completeExceptionally(cause);
		} else {
			unhandled(msg);
		}
	}

	@Override
	protected CompletableFuture<byte[]> next() {
		future = new CompletableFuture<>();
		
		t.read(buffer, position, getSelf(), new CompletionHandler<Integer, ActorRef>() {

			@Override
			public void completed(Integer result, ActorRef self) {
				log.debug("read completed: {}", result);
				
				if(result == -1) {
					self.tell(new Eof(), self);
					self.tell(new byte[0], self);
				} else {
					buffer.flip();
					
					byte[] chunk = new byte[result];
					buffer.get(chunk);
					
					self.tell(chunk, self);
					
					buffer.clear();
				}
			}

			@Override
			public void failed(Throwable exc, ActorRef self) {
				log.debug("read failed");
				
				self.tell(new Failure(exc), self);
			}
		});
		
		return future;
	}

}
