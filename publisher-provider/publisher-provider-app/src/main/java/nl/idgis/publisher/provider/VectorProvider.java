package nl.idgis.publisher.provider;

import java.util.Optional;

import nl.idgis.publisher.provider.protocol.GetVectorDataset;

import akka.actor.ActorRef;
import akka.actor.Props;

public class VectorProvider extends AbstractProvider {
		
	private final ActorRef database;
	
	public VectorProvider(ActorRef database, DatasetInfoSourceDesc datasetInfoSourceDesc) {
		super(datasetInfoSourceDesc);
		
		this.database = database;		
	}
	
	public static Props props(ActorRef database, DatasetInfoSourceDesc datasetInfoSourceDesc) {
		return Props.create(VectorProvider.class, database, datasetInfoSourceDesc);
	}
	
	@Override
	protected Optional<Props> getVectorDatasetFetcher(GetVectorDataset msg) {
		return Optional.of(VectorDatasetFetcher.props(getSender(), database, msg));
	}
	
	@Override
	protected DatasetInfoBuilderPropsFactory getDatasetInfoBuilder() {
		return MetadataItemVectorDatasetInfoBuilder.props(database);
	}
}
