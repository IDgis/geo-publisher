package nl.idgis.publisher.provider.sde;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.service.DatabaseLog;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.domain.service.FileLog;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.folder.messages.FileNotExists;
import nl.idgis.publisher.folder.messages.FileSize;
import nl.idgis.publisher.folder.messages.GetFileSize;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.ProviderUtils;
import nl.idgis.publisher.provider.database.messages.DatabaseColumnInfo;
import nl.idgis.publisher.provider.database.messages.DatabaseTableInfo;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.ColumnInfo;
import nl.idgis.publisher.provider.protocol.RasterDatasetInfo;
import nl.idgis.publisher.provider.protocol.RasterFormat;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.xml.exceptions.NotFound;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class SDEGatherDatasetInfo extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final ActorRef target;
	
	private final ActorRef transaction;
	
	private final ActorRef rasterFolder;
	
	private final Set<AttachmentType> attachmentTypes;
	
	private SDEItemInfo itemInfo;
	
	private String identification;
	
	private String title;
	
	private String alternateTitle;
	
	private String physicalname;
	
	private String categoryId;
	
	private DatabaseTableInfo databaseTableInfo;

	private Path rasterFile;
	
	private Set<Attachment> attachments;
	
	private Date revisionDate;
	
	private Map<String, String> attributeAliases;
	
	public SDEGatherDatasetInfo(ActorRef target, ActorRef transaction, ActorRef rasterFolder, Set<AttachmentType> attachmentTypes) {
		this.target = target;
		this.transaction = transaction;
		this.rasterFolder = rasterFolder;
		this.attachmentTypes = attachmentTypes;
	}
	
	public static Props props(ActorRef target, ActorRef transaction, ActorRef rasterFolder, Set<AttachmentType> attachmentTypes) {
		return Props.create(SDEGatherDatasetInfo.class, target, transaction, rasterFolder, attachmentTypes);
	}
	
	@Override
	public void preStart() {
		 attachments = new HashSet<>();
		 attributeAliases = Collections.emptyMap();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof FileNotExists) {
			log.debug("file not exists: {}", msg);
			
			Set<Log> logs = new HashSet<>();
			logs.add(Log.create(
				LogLevel.ERROR, 
				DatasetLogType.FILE_NOT_FOUND, 
				new FileLog(physicalname)));
			
			target.tell(
				new UnavailableDatasetInfo(
					identification,
					title,
					alternateTitle,
					categoryId,
					revisionDate,
					attachments,
					logs),
				getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof FileSize) {
			log.debug("file size received: {}", msg);
			
			target.tell(
				new RasterDatasetInfo(
					identification, 
					title, 
					alternateTitle, 
					categoryId, 
					revisionDate, 
					attachments,
					Collections.emptySet(), // logs
					RasterFormat.TIFF,
					((FileSize)msg).getSize()),
				getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof SDEItemInfo) {
			log.debug("item info received: {}", msg);
			
			itemInfo = (SDEItemInfo)msg;
			identification = itemInfo.getUuid();
			physicalname = itemInfo.getPhysicalname();
			categoryId = ProviderUtils.getCategoryId(physicalname);
			
			title = physicalname;
			itemInfo.getDocumentation().ifPresent(documentation -> {
				try {
					MetadataDocumentFactory mdf = new MetadataDocumentFactory();
					MetadataDocument md = mdf.parseDocument(documentation.getBytes("utf-8"));
					
					attributeAliases = md.getAttributeAliases();
					revisionDate = md.getDatasetRevisionDate();
					
					if(attachmentTypes.contains(AttachmentType.METADATA)) {
						attachments.add(new Attachment(
							"gdb_items_vw.documentation", 
							AttachmentType.METADATA,
							md.getContent()));
					}
				
					try {
						title = md.getDatasetTitle();
					} catch(NotFound nf) {}
					try {
						alternateTitle = md.getDatasetAlternateTitle();
					} catch(NotFound nf) {}
				} catch(Exception e) {
					log.error(e, "couldn't process documentation content");
				}
			});
			
			SDEItemInfoType type = itemInfo.getType();
			switch(type) {
				case RASTER_DATASET:
					rasterFile = Paths.get(physicalname + ".tif");
					rasterFolder.tell(new GetFileSize(rasterFile), getSelf());
					break;
				case TABLE:
				case FEATURE_CLASS:
					transaction.tell(new DescribeTable(physicalname), getSelf());
					break;
				default:
					log.error("unknown item type: {}", type);
					getContext().stop(getSelf());
			}
		} else if(msg instanceof Long) {
			long numberOfRecords = (Long)msg;
			log.debug("count received: {}", numberOfRecords);
			
			ArrayList<ColumnInfo> columns = new ArrayList<>();
			for(DatabaseColumnInfo columnInfo : databaseTableInfo.getColumns()) {
				Type columnType = columnInfo.getType();
				
				if(columnType == null) {
					log.debug("unknown data type: " + columnInfo.getTypeName());
				} else {
					String columnName = columnInfo.getName();
					String columnAlias = attributeAliases.get(columnName);
					
					columns.add(new ColumnInfo(columnName, columnType, columnAlias));
				}
			}
			
			TableInfo tableInfo = new TableInfo(columns.toArray(new ColumnInfo[columns.size()]));
			
			log.debug("sending vector dataset info");
			
			target.tell( 
				new VectorDatasetInfo(
					identification, 
					title, 
					alternateTitle,
					categoryId,
					revisionDate, 
					attachments,
					Collections.emptySet(), // logs
					physicalname, // tableName
					tableInfo,
					numberOfRecords),
				getSelf());
			
			getContext().stop(getSelf());
		} else if(msg instanceof DatabaseTableInfo) {
			databaseTableInfo = (DatabaseTableInfo)msg;
			log.debug("table info received: {}", databaseTableInfo);
			
			getSender().tell(new PerformCount(physicalname), getSelf());
		} else if(msg instanceof TableNotFound) {
			log.debug("table not found -> sending unavailable dataset info");
			
			Set<Log> logs = new HashSet<>();
			logs.add(Log.create(LogLevel.ERROR, DatasetLogType.TABLE_NOT_FOUND, new DatabaseLog(physicalname)));
			
			target.tell(
				new UnavailableDatasetInfo(
					identification,
					title, 
					alternateTitle,
					categoryId,
					revisionDate,
					attachments,
					logs),
				getSelf());
			
			getContext().stop(getSelf());
		}
	}
}
