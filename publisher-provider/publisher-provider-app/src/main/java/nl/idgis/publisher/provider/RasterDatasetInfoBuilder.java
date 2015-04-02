package nl.idgis.publisher.provider;

import java.util.Set;

import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.RasterDatasetInfo;
import nl.idgis.publisher.provider.protocol.RasterFormat;

import akka.actor.ActorRef;
import akka.actor.Props;

public class RasterDatasetInfoBuilder extends AbstractDatasetInfoBuilder {

	public RasterDatasetInfoBuilder(ActorRef sender, ActorRef converter, Set<AttachmentType> requestedAttachmentTypes) {
		super(sender, converter, requestedAttachmentTypes);
	}
	
	public static DatasetInfoBuilderPropsFactory props() {
		return (sender, converter, requestedAttachmentTypes) ->
			Props.create(RasterDatasetInfoBuilder.class, sender, converter, requestedAttachmentTypes);
	}

	@Override
	protected void processMetadata() {
		categoryId = "raster";
		if(logs.isEmpty()) {
			tellTarget(new RasterDatasetInfo(identification, reportedTitle, alternateTitle, categoryId, revisionDate, attachments, logs, RasterFormat.TIFF));
		} else {
			sendUnavailable();
		}
	}
}
