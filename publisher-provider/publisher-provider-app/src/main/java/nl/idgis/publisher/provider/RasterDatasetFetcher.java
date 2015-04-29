package nl.idgis.publisher.provider;

import java.nio.file.Paths;

import nl.idgis.publisher.folder.messages.FetchFile;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.provider.protocol.GetRasterDataset;

import akka.actor.ActorRef;
import akka.actor.Props;

public class RasterDatasetFetcher extends AbstractDatasetFetcher<GetRasterDataset> {
	
	private final ActorRef folder;

	public RasterDatasetFetcher(ActorRef sender, ActorRef folder, GetRasterDataset request) {
		super(sender, request);
		
		this.folder = folder;
	}
	
	public static Props props(ActorRef sender, ActorRef folder, GetRasterDataset request) {
		return Props.create(RasterDatasetFetcher.class, sender, folder, request);
	}

	@Override
	protected void handleMetadataDocument(MetadataDocument metadataDocument) throws Exception {
		String fileName = ProviderUtils.getRasterFile(metadataDocument.getAlternateTitle());		
		folder.tell(new FetchFile(Paths.get(fileName)), sender);
	}

}
