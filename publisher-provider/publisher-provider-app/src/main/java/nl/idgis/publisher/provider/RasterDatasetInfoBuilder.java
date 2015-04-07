package nl.idgis.publisher.provider;

import java.nio.file.Paths;
import java.util.Set;

import nl.idgis.publisher.folder.messages.FileSize;
import nl.idgis.publisher.folder.messages.GetFileSize;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.RasterDatasetInfo;
import nl.idgis.publisher.provider.protocol.RasterFormat;

import akka.actor.ActorRef;
import akka.actor.Props;

public class RasterDatasetInfoBuilder extends AbstractDatasetInfoBuilder {
	
	private final ActorRef folder;

	public RasterDatasetInfoBuilder(ActorRef sender, ActorRef converter, ActorRef folder, Set<AttachmentType> requestedAttachmentTypes) {
		super(sender, converter, requestedAttachmentTypes);
		
		this.folder = folder;
	}
	
	public static DatasetInfoBuilderPropsFactory props(ActorRef folder) {
		return (sender, converter, requestedAttachmentTypes) ->
			Props.create(RasterDatasetInfoBuilder.class, sender, converter, folder, requestedAttachmentTypes);
	}

	@Override
	protected void processMetadata() {
		categoryId = "raster";
		
		if(title == null) {
			reportedTitle = alternateTitle;
		} else {
			reportedTitle = title;
		}
		
		if(logs.isEmpty()) {
			folder.tell(new GetFileSize(Paths.get(ProviderUtils.getRasterFile(alternateTitle))), getSelf());
		} else {
			sendUnavailable();
		}
	}

	@Override
	protected void onReceiveElse(Object msg) throws Exception {
		if(msg instanceof FileSize) {
			tellTarget(new RasterDatasetInfo(identification, reportedTitle, alternateTitle, categoryId, revisionDate, attachments, logs, confidential, RasterFormat.TIFF, ((FileSize)msg).getSize()));
		} else {
			unhandled(msg);
		}
	}
	
	
}
