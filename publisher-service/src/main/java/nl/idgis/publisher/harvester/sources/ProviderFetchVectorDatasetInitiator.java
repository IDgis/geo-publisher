package nl.idgis.publisher.harvester.sources;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.harvester.sources.messages.FetchVectorDataset;
import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.harvester.sources.messages.StartVectorImport;
import nl.idgis.publisher.provider.protocol.AbstractGetDatasetRequest;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;

public class ProviderFetchVectorDatasetInitiator extends ProviderFetchDatasetInitiator<FetchVectorDataset, VectorDatasetInfo> {
	
	private final int GET_VECTOR_DATASET_MESSAGE_SIZE = 10;
	
	public ProviderFetchVectorDatasetInitiator(ActorRef sender, FetchVectorDataset request, ActorRef receiver, ActorRef provider) {
		super(sender, request, receiver, provider);
	}
	
	public static Props props(ActorRef sender, FetchVectorDataset request, ActorRef receiver, ActorRef provider) {
		return Props.create(ProviderFetchVectorDatasetInitiator.class, sender, request, receiver, provider);
	}
	
	@Override
	protected StartImport startImport(VectorDatasetInfo vectorDatasetInfo) {
		return new StartVectorImport(sender, vectorDatasetInfo.getNumberOfRecords());
	}
	
	@Override
	protected AbstractGetDatasetRequest getDataset() {
		return new GetVectorDataset(request.getId(), request.getColumns(), GET_VECTOR_DATASET_MESSAGE_SIZE);
	}
	
}
