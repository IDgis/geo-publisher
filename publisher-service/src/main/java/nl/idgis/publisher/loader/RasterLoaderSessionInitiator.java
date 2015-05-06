package nl.idgis.publisher.loader;

import java.nio.file.Paths;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.folder.messages.FileReceiver;
import nl.idgis.publisher.folder.messages.GetFileReceiver;
import nl.idgis.publisher.harvester.sources.messages.FetchRasterDataset;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.RasterImportJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;

public class RasterLoaderSessionInitiator extends AbstractLoaderSessionInitiator<RasterImportJobInfo> {
	
	private final ActorRef rasterFolder;
	
	private ActorRef receiver;

	protected RasterLoaderSessionInitiator(RasterImportJobInfo importJob, ActorRef jobContext, ActorRef rasterFolder, Duration receiveTimeout) {
		super(importJob, jobContext, receiveTimeout);
		
		this.rasterFolder = rasterFolder;
	}
	
	public static Props props(RasterImportJobInfo importJob, ActorRef jobContext, ActorRef rasterFolder) {
		return props(importJob, jobContext, rasterFolder, DEFAULT_RECEIVE_TIMEOUT);
	}
	
	public static Props props(RasterImportJobInfo importJob, ActorRef jobContext, ActorRef rasterFolder, Duration receiveTimeout) {
		return Props.create(RasterLoaderSessionInitiator.class, importJob, jobContext, rasterFolder, receiveTimeout);
	}

	@Override
	protected void dataSourceReceived() throws Exception {
		jobContext.tell(new UpdateJobState(JobState.STARTED), getSelf());		
		become("storing started job state", waitingForJobStartedStored());
	}

	private void startLoaderSession() throws Exception {
		startLoaderSession(new FetchRasterDataset(
			importJob.getExternalSourceDatasetId(), 
			RasterLoaderSession.props(
				getContext().parent(), // loader
				importJob,
				jobContext,
				receiver)));
	}
	
	private Procedure<Object> waitingForReceiver() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception { 
				if(msg instanceof FileReceiver) {
					receiver = ((FileReceiver)msg).getReceiver();
					startLoaderSession();
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private void startReceiver() {
		String fileName = importJob.getDatasetId() + ".tif";		
		rasterFolder.tell(new GetFileReceiver(Paths.get(fileName)), getSelf());
		become("waiting for receiver", waitingForReceiver());
	}
	
	private Procedure<Object> waitingForJobStartedStored() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					startReceiver();
				} else {
					unhandled(msg);
				}
			}
		};
	}

}
