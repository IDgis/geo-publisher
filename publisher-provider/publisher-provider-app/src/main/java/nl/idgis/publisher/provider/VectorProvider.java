package nl.idgis.publisher.provider;

import java.util.Optional;

import akka.actor.ActorRef;
import akka.actor.Props;
import nl.idgis.publisher.provider.database.DatabaseType;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;

public class VectorProvider extends AbstractProvider {
	
	private final DatabaseType databaseVendor;
	
	private final Props databaseProps;
	
	private ActorRef database;
	
	public VectorProvider(Props databaseProps, Props metadataProps, DatabaseType databaseVendor) {
		super(metadataProps);
		
		this.databaseProps = databaseProps;	
		this.databaseVendor = databaseVendor;
	}
	
	public static Props props(Props databaseProps, Props metadataProps, DatabaseType databaseVendor) {
		return Props.create(VectorProvider.class, databaseProps, metadataProps, databaseVendor);
	}
	
	@Override
	public void preStartProvider() {
		database = getContext().actorOf(databaseProps, "database");
	}
	
	@Override
	protected Optional<Props> getVectorDatasetFetcher(GetVectorDataset msg) {
		return Optional.of(VectorDatasetFetcher.props(getSender(), database, databaseVendor, msg));
	}
	
	@Override
	protected DatasetInfoBuilderPropsFactory getDatasetInfoBuilder() {
		return VectorDatasetInfoBuilder.props(database);
	}
}
