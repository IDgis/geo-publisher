package nl.idgis.publisher.loader;

import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.harvester.sources.messages.StartRasterImport;
import nl.idgis.publisher.job.manager.messages.RasterImportJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;

public class RasterLoaderSession extends AbstractLoaderSession<RasterImportJobInfo, StartRasterImport> {
	
	private long progress = 0;
	
	private final ActorRef receiver; 
	
	public RasterLoaderSession(Duration receiveTimeout, int maxRetries, ActorRef loader, RasterImportJobInfo importJob, ActorRef jobContext, ActorRef receiver) {
		super(receiveTimeout, maxRetries, loader, importJob, jobContext);
		
		this.receiver = receiver;
	}
	
	public static Props props(ActorRef loader, RasterImportJobInfo importJob, ActorRef jobContext, ActorRef receiver) {
		return props(DEFAULT_RECEIVE_TIMEOUT, DEFAULT_MAX_RETRIES, loader, importJob, jobContext, receiver);
	}
	
	public static Props props(Duration receiveTimeout, int maxRetries, ActorRef loader, RasterImportJobInfo importJob, ActorRef jobContext, ActorRef receiver) {
		return Props.create(RasterLoaderSession.class, receiveTimeout, maxRetries, loader, importJob, jobContext, receiver);
	}

	@Override
	protected long progress() {
		return progress;
	}

	@Override
	protected long progressTarget(StartRasterImport startImport) {
		return startImport.getSize();
	}

	@Override
	protected void handleItemContent(Object content) throws Exception {
		if(content instanceof byte[]) {
			handleFileChunk((byte[])content);
		} else {
			log.error("unknown item content: {}" + content);
		}
	}
	
	private Procedure<Object> waitingForAck(ActorRef producer) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					producer.tell(new NextItem(), getSelf());
					getContext().unbecome();
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private void handleFileChunk(byte[] content) {
		log.debug("file chunk received, size: {}", content.length);
		
		progress += content.length;
		updateProgress();
		
		receiver.tell(content, getSelf());
		
		getContext().become(waitingForAck(getSender()), false);
	}

	@Override
	protected CompletableFuture<Object> importFailed() {
		return f.ask(receiver, new End(), Ack.class).thenApply(ack -> {
			log.debug("closed");
			
			return ack;
		});
	}

	@Override
	protected CompletableFuture<Object> importSucceeded() {
		return f.ask(receiver, new End(), Ack.class).thenApply(ack -> {
			log.debug("closed");
			
			return ack;
		});
	}

}
