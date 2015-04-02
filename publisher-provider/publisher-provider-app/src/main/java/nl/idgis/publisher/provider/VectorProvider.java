package nl.idgis.publisher.provider;

import java.util.Optional;

import nl.idgis.publisher.provider.protocol.GetDatasetInfo;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;

import akka.actor.ActorRef;
import akka.actor.Props;

public class VectorProvider extends AbstractProvider {
	
	private final Props databaseProps;
	
	private ActorRef database;
	
	public VectorProvider(Props databaseProps, Props metadataProps) {
		super(metadataProps);
		
		this.databaseProps = databaseProps;		
	}
	
	public static Props props(Props databaseProps, Props metadataProps) {
		return Props.create(VectorProvider.class, databaseProps, metadataProps);
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
	protected Props getDatasetInfoBuilder(GetDatasetInfo msg) {
		return DatasetInfoBuilder.props(getSender(), getSelf(), database, msg.getAttachmentTypes());
	}
	
	@Override
	protected Props getDatasetInfoConverter(ListDatasetInfo msg) {
		return DatasetInfoConverter.props(msg.getAttachmentTypes(), metadata, database);
	}
}
