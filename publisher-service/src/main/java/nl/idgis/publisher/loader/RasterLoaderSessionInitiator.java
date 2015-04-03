package nl.idgis.publisher.loader;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Procedure;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.harvester.sources.messages.FetchRasterDataset;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.RasterImportJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;

public class RasterLoaderSessionInitiator extends AbstractLoaderSessionInitiator<RasterImportJobInfo> {
	
	private final ActorRef rasterFolder;

	protected RasterLoaderSessionInitiator(RasterImportJobInfo importJob, ActorRef jobContext, ActorRef rasterFolder) {
		super(importJob, jobContext);
		
		this.rasterFolder = rasterFolder;
	}
	
	public static Props props(RasterImportJobInfo importJob, ActorRef jobContext, ActorRef rasterFolder) {
		return Props.create(RasterLoaderSessionInitiator.class, importJob, jobContext, rasterFolder);
	}

	@Override
	protected void dataSourceReceived() throws Exception {
		jobContext.tell(new UpdateJobState(JobState.STARTED), getSelf());		
		become("storing started job state", waitingForJobStartedStored());
	}

	private void startLoaderSession() throws Exception {
		startLoaderSession(new FetchRasterDataset(
			importJob.getSourceDatasetId(), 
			RasterLoaderSession.props(
				getContext().parent(), // loader
				importJob,
				jobContext)));
	}
	
	private Procedure<Object> waitingForJobStartedStored() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					startLoaderSession();
				} else {
					unhandled(msg);
				}
			}
		};
	}

}
