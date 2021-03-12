package nl.idgis.publisher.provider.sde;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
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
import nl.idgis.publisher.provider.database.DatabaseType;
import nl.idgis.publisher.provider.database.messages.DatabaseTableInfo;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.provider.database.messages.AbstractDatabaseColumnInfo;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.ColumnInfo;
import nl.idgis.publisher.provider.protocol.RasterDatasetInfo;
import nl.idgis.publisher.provider.protocol.RasterFormat;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.xml.exceptions.NotFound;

public class SDEGatherDatasetInfo extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final ActorRef target;
	
	private final ActorRef transaction;
	
	private final ActorRef rasterFolder;
	
	private final Set<AttachmentType> attachmentTypes;

	private String identification;
	
	private String title;
	
	private String alternateTitle;
	
	private String physicalname;
	
	private String categoryId;
	
	private String databaseScheme;

	private String metadataTable;

	private DatabaseTableInfo databaseTableInfo;

	private Set<Attachment> attachments;
	
	private ZonedDateTime revisionDate;
	
	private Map<String, String> attributeAliases;

	private Config rasterConfig;
	
	public SDEGatherDatasetInfo(ActorRef target, ActorRef transaction, ActorRef rasterFolder, Set<AttachmentType> attachmentTypes, Config databaseConfig, Config rasterConfig) {
		this.target = target;
		this.transaction = transaction;
		this.rasterFolder = rasterFolder;
		this.attachmentTypes = attachmentTypes;
		this.rasterConfig = rasterConfig;

		try {
			this.databaseScheme = databaseConfig.getString("scheme");
		} catch(ConfigException.Missing cem) {
			this.databaseScheme = "SDE";
		}

		if (databaseConfig.hasPath("vendor")) {
			try {
				DatabaseType databaseVendor = DatabaseType.valueOf(databaseConfig.getString("vendor").toUpperCase());
				if (databaseVendor == DatabaseType.POSTGRES) {
					this.metadataTable = "gdb_items";
				} else {
					this.metadataTable = "GDB_ITEMS_VW";
				}
			} catch(IllegalArgumentException iae) {
				throw new ConfigException.BadValue("vendor", "Invalid vendor supplied in config");
			}
		} else {
			this.metadataTable = "GDB_ITEMS_VW";
		}
	}
	
	public static Props props(ActorRef target, ActorRef transaction, ActorRef rasterFolder, Set<AttachmentType> attachmentTypes, Config databaseConfig, Config rasterConfig) {
		return Props.create(SDEGatherDatasetInfo.class, target, transaction, rasterFolder, attachmentTypes, databaseConfig, rasterConfig);
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
			
			try {
				String rasterPath = rasterConfig.getString("folder");
				
				Path root = Paths.get(rasterPath);
				Path dateFile = root.resolve(Paths.get(physicalname + "_date.txt"));
				
				byte[] dateFileContent = Files.readAllBytes(dateFile);
				
				StringBuilder sb = new StringBuilder(new String(dateFileContent));
				String dateString = sb.substring(0, 10);
				
				LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
				revisionDate = date.atStartOfDay(ZoneId.of("Europe/Amsterdam"));
			} catch(ConfigException ce) {
				log.debug("no or wrong raster folder in config");
			} catch(InvalidPathException ipe) {
				log.debug("something went wrong in getting path of sde raster date file");
			} catch(IOException ioe) {
				log.debug("couldn't read sde raster date file");
			} catch(Exception e) {
				log.debug("setting of sde raster date went wrong because of unknown reason");
			}
			
			RasterDatasetInfo rasterDatasetInfo = new RasterDatasetInfo(
					identification, 
					title, 
					alternateTitle, 
					categoryId, 
					revisionDate, 
					attachments,
					Collections.emptySet(), // logs
					RasterFormat.TIFF,
					((FileSize)msg).getSize());
			
			log.debug("sending raster dataset info: {}", rasterDatasetInfo);
			
			target.tell(rasterDatasetInfo, getSelf());
			
			getContext().stop(getSelf());
		} else if(msg instanceof SDEItemInfo) {
			log.debug("item info received: {}", msg);

			SDEItemInfo itemInfo = (SDEItemInfo) msg;
			identification = itemInfo.getUuid();
			physicalname = itemInfo.getPhysicalname();
			categoryId = ProviderUtils.getCategoryId(physicalname);
			
			title = physicalname;

			log.debug("database scheme in sde gather dataset info: " + databaseScheme + "." + metadataTable);

			itemInfo.getDocumentation().ifPresent(documentation -> {
				try {
					MetadataDocumentFactory mdf = new MetadataDocumentFactory();
					MetadataDocument md = mdf.parseDocument(documentation.getBytes("utf-8"));
					
					attributeAliases = md.getAttributeAliases();
					try {
						revisionDate = md.getDatasetRevisionDate();
					} catch (NotFound nf) {}
					
					if(attachmentTypes.contains(AttachmentType.METADATA)) {
						attachments.add(new Attachment(
							databaseScheme + "." + metadataTable + ".documentation",
							AttachmentType.METADATA,
							md.getContent()));
					}
					
					if(attachmentTypes.contains(AttachmentType.PHYSICAL_NAME)) {
						attachments.add(new Attachment(
							databaseScheme + "." +metadataTable + ".physicalname",
							AttachmentType.PHYSICAL_NAME,
							physicalname));
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
					Path rasterFile = Paths.get(physicalname + ".tif");
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
			for(AbstractDatabaseColumnInfo columnInfo : databaseTableInfo.getColumns()) {
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
			
			VectorDatasetInfo vectorDatasetInfo = 
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
					numberOfRecords);
			
			log.debug("sending vector dataset info: {}", vectorDatasetInfo);
			
			target.tell(vectorDatasetInfo, getSelf());
			
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
