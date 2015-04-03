package nl.idgis.publisher.loader;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.harvester.sources.messages.FetchRasterDataset;
import nl.idgis.publisher.job.manager.messages.RasterImportJobInfo;

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
		startLoaderSession(new FetchRasterDataset(
			importJob.getSourceDatasetId(), 
			RasterLoaderSession.props(
				getContext().parent(), // loader
				importJob,
				jobContext)));
	}

}
