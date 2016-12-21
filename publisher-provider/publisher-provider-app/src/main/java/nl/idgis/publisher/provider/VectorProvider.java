package nl.idgis.publisher.provider;

import java.util.Optional;

import nl.idgis.publisher.provider.protocol.GetVectorDataset;

import akka.actor.ActorRef;
import akka.actor.Props;

public class VectorProvider extends AbstractProvider {
	
	private final Props databaseProps;
	
	private ActorRef database;
	
	public VectorProvider(Props databaseProps, DatasetInfoSourceDesc datasetInfoSourceDesc) {
		super(datasetInfoSourceDesc);
		
		this.databaseProps = databaseProps;		
	}
	
	public static Props props(Props databaseProps, DatasetInfoSourceDesc datasetInfoSourceDesc) {
		return Props.create(VectorProvider.class, databaseProps, datasetInfoSourceDesc);
	}
	
	@Override
	public void preStartProvider() {
		database = getContext().actorOf(databaseProps, "database");		
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
