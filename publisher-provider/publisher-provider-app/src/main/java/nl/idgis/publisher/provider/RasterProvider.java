package nl.idgis.publisher.provider;

import java.util.Optional;

import nl.idgis.publisher.provider.protocol.GetRasterDataset;

import akka.actor.ActorRef;
import akka.actor.Props;

public class RasterProvider extends AbstractProvider {
	
	private final Props folderProps;
	
	private ActorRef folder;
	
	public RasterProvider(Props folderProps, Props metadataProps) {
		super(metadataProps);
		
		this.folderProps = folderProps;		
	}

	public static Props props(Props folderProps, Props metadataProps) {
		return Props.create(RasterProvider.class, folderProps, metadataProps);
	}
	
	@Override
	protected void preStartProvider() throws Exception {
		folder = getContext().actorOf(folderProps);
	}

	@Override
	protected DatasetInfoBuilderPropsFactory getDatasetInfoBuilder() { 
		return RasterDatasetInfoBuilder.props();
	}

	@Override
	protected Optional<Props> getRasterDatasetFetcher(GetRasterDataset msg) { 
		return Optional.of(RasterDatasetFetcher.props(getSender(), folder, msg));
	}
	
}
