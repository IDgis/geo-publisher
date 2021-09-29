package nl.idgis.publisher.provider;

import java.util.Optional;

import nl.idgis.publisher.provider.protocol.GetVectorDataset;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;

public class VectorProvider extends AbstractProvider {
	
	private final Props databaseProps;
	private final Config databaseConfig;
	
	private ActorRef database;
	
	public VectorProvider(Props databaseProps, Props metadataProps, Config databaseConfig) {
		super(metadataProps);
		
		this.databaseProps = databaseProps;
		this.databaseConfig = databaseConfig;
	}
	
	public static Props props(Props databaseProps, Props metadataProps, Config databaseConfig) {
		return Props.create(VectorProvider.class, databaseProps, metadataProps, databaseConfig);
	}
	
	@Override
	public void preStartProvider() {
		database = getContext().actorOf(databaseProps, "database");		
	}
	
	@Override
	protected Optional<Props> getVectorDatasetFetcher(GetVectorDataset msg) {
		return Optional.of(VectorDatasetFetcher.props(getSender(), database, msg, databaseConfig));
	}
	
	@Override
	protected DatasetInfoBuilderPropsFactory getDatasetInfoBuilder() {
		return VectorDatasetInfoBuilder.props(database);
	}
}
