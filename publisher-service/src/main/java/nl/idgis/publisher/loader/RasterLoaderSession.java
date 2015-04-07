package nl.idgis.publisher.loader;

import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.folder.messages.FileChunk;
import nl.idgis.publisher.harvester.sources.messages.StartRasterImport;
import nl.idgis.publisher.job.manager.messages.RasterImportJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;

public class RasterLoaderSession extends AbstractLoaderSession<RasterImportJobInfo, StartRasterImport> {
	
	private long progress = 0;
	
	private final ActorRef receiver; 
	
	public RasterLoaderSession(Duration receiveTimeout, ActorRef loader, RasterImportJobInfo importJob, ActorRef jobContext, ActorRef receiver) {
		super(receiveTimeout, loader, importJob, jobContext);
		
		this.receiver = receiver;
	}
	
	public static Props props(ActorRef loader, RasterImportJobInfo importJob, ActorRef jobContext, ActorRef receiver) {
		return props(DEFAULT_RECEIVE_TIMEOUT, loader, importJob, jobContext, receiver);
	}
	
	public static Props props(Duration receiveTimeout, ActorRef loader, RasterImportJobInfo importJob, ActorRef jobContext, ActorRef receiver) {
		return Props.create(RasterLoaderSession.class, receiveTimeout, loader, importJob, jobContext, receiver);
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
	protected Procedure<Object> importing() {		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof FileChunk) {
					handleFileChunk((FileChunk)msg);
				} else {
					onReceiveElse(msg);
				}
			}

			
		};
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
	
	private void handleFileChunk(FileChunk msg) {
		byte[] content = msg.getContent();
		
		log.debug("file chunk received, size: {}", content.length);
		
		progress += content.length;
		updateProgress();
		
		receiver.tell(msg, getSelf());
		
		getContext().become(waitingForAck(getSender()), false);
	}

	@Override
	protected CompletableFuture<Object> importFailed() {
		return f.ask(receiver, new End(), Ack.class).thenApply(ack -> {
			log.debug("closed");
			
			return null;
		});
	}

	@Override
	protected CompletableFuture<Object> importSucceeded() {
		return f.ask(receiver, new End(), Ack.class).thenApply(ack -> {
			log.debug("closed");
			
			return null;
		});
	}

}
