package nl.idgis.publisher.provider;

import java.nio.file.Paths;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.domain.service.FileLog;
import nl.idgis.publisher.folder.messages.FileNotExists;
import nl.idgis.publisher.folder.messages.FileSize;
import nl.idgis.publisher.folder.messages.GetFileSize;
import nl.idgis.publisher.provider.messages.SkipDataset;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.RasterDatasetInfo;
import nl.idgis.publisher.provider.protocol.RasterFormat;

import akka.actor.ActorRef;
import akka.actor.Props;

public class RasterDatasetInfoBuilder extends AbstractDatasetInfoBuilder {
	
	private final ActorRef folder;
	
	private String fileName;

	public RasterDatasetInfoBuilder(ActorRef sender, ActorRef folder, Set<AttachmentType> requestedAttachmentTypes) {
		super(sender, requestedAttachmentTypes);
		
		this.folder = folder;
	}
	
	public static DatasetInfoBuilderPropsFactory props(ActorRef folder) {
		return (sender, requestedAttachmentTypes) ->
			Props.create(RasterDatasetInfoBuilder.class, sender, folder, requestedAttachmentTypes);
	}

	@Override
	protected void processMetadata() {
		if(!"grid".equals(spatialRepresentationType)) {
			tellTarget(new SkipDataset(identification));
			return;
		}
		
		categoryId = "raster";
		
		if(alternateTitle != null && !alternateTitle.trim().isEmpty()) {
			fileName = ProviderUtils.getRasterFile(alternateTitle);
			
			if(requestedAttachmentTypes.contains(AttachmentType.PHYSICAL_NAME)) {
				attachments.add(new Attachment(identification, AttachmentType.PHYSICAL_NAME, fileName));
			}
			
			folder.tell(new GetFileSize(Paths.get(fileName)), getSelf());
		} else {
			logs.add(Log.create(LogLevel.ERROR, DatasetLogType.UNKNOWN_FILE));
			
			sendUnavailable();
		}
	}

	@Override
	protected void onReceiveElse(Object msg) throws Exception {
		if(msg instanceof FileSize) {
			if(logs.isEmpty()) {
				tellTarget(new RasterDatasetInfo(identification, reportedTitle, alternateTitle, categoryId, revisionDate, attachments, logs, RasterFormat.TIFF, ((FileSize)msg).getSize()));
			} else {
				sendUnavailable();
			}
		} else if(msg instanceof FileNotExists) {
			log.debug("file not found");
			
			logs.add(Log.create(LogLevel.ERROR, DatasetLogType.FILE_NOT_FOUND, new FileLog(fileName)));
			
			sendUnavailable();
		} else {
			unhandled(msg);
		}
	}
	
	
}
