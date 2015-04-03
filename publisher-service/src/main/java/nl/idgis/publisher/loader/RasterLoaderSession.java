package nl.idgis.publisher.loader;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.harvester.sources.messages.StartRasterImport;
import nl.idgis.publisher.job.manager.messages.RasterImportJobInfo;
import nl.idgis.publisher.provider.protocol.FileChunk;

public class RasterLoaderSession extends AbstractLoaderSession<RasterImportJobInfo, StartRasterImport> {
	
	private long progress = 0;
	
	public RasterLoaderSession(Duration receiveTimeout, ActorRef loader, RasterImportJobInfo importJob, ActorRef jobContext) {
		super(receiveTimeout, loader, importJob, jobContext);
	}
	
	public static Props props(ActorRef loader, RasterImportJobInfo importJob, ActorRef jobContext) {
		return props(DEFAULT_RECEIVE_TIMEOUT, loader, importJob, jobContext);
	}
	
	public static Props props(Duration receiveTimeout, ActorRef loader, RasterImportJobInfo importJob, ActorRef jobContext) {
		return Props.create(RasterLoaderSession.class, receiveTimeout, loader, importJob, jobContext);
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
	
	private void handleFileChunk(FileChunk msg) {
		byte[] content = msg.getContent();
		
		log.debug("file chunk received, size: {}", content.length);
		
		progress += content.length;
		updateProgress();
	}

}
