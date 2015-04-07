package nl.idgis.publisher.harvester.sources;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.harvester.sources.messages.FetchRasterDataset;
import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.harvester.sources.messages.StartRasterImport;
import nl.idgis.publisher.provider.protocol.AbstractGetDatasetRequest;
import nl.idgis.publisher.provider.protocol.GetRasterDataset;
import nl.idgis.publisher.provider.protocol.RasterDatasetInfo;

public class ProviderFetchRasterDatasetInitiator extends ProviderFetchDatasetInitiator<FetchRasterDataset, RasterDatasetInfo> {

	public ProviderFetchRasterDatasetInitiator(ActorRef sender, FetchRasterDataset request, ActorRef receiver, ActorRef provider) {
		super(sender, request, receiver, provider);
	}
	
	public static Props props(ActorRef sender, FetchRasterDataset request, ActorRef receiver, ActorRef provider) {
		return Props.create(ProviderFetchRasterDatasetInitiator.class, sender, request, receiver, provider);
	}

	@Override
	protected StartImport startImport(RasterDatasetInfo datasetInfo) {
		return new StartRasterImport(sender, datasetInfo.getSize());		
	}

	@Override
	protected AbstractGetDatasetRequest getDataset() {
		return new GetRasterDataset(request.getId());
	}
}
