package nl.idgis.publisher.provider;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.harvest.MetadataField;
import nl.idgis.publisher.domain.job.harvest.MetadataLogType;
import nl.idgis.publisher.domain.service.DatabaseLog;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.domain.service.MetadataLog;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.database.messages.TableNotFound;
import nl.idgis.publisher.provider.metadata.messages.MetadataItem;
import nl.idgis.publisher.provider.metadata.messages.MetadataNotFound;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.DatasetNotFound;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.xml.exceptions.NotFound;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class DatasetInfoBuilder extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef sender, converter, database;
	
	private final Set<AttachmentType> requestedAttachmentTypes;
	
	private final Set<Attachment> attachments;
	
	private final Set<Log> logs;
	
	private MetadataDocumentFactory metadataDocumentFactory;
	
	private String identification;
	
	private String title, alternateTitle, tableName, reportedTitle, categoryId;
	
	private Date revisionDate;
	
	private TableInfo tableInfo;
	
	private Long numberOfRecords;

	public DatasetInfoBuilder(ActorRef sender, ActorRef converter, ActorRef database, Set<AttachmentType> requestedAttachmentTypes) {			
		this.sender = sender;		
		this.converter = converter;
		this.database = database;
		this.requestedAttachmentTypes = requestedAttachmentTypes;
		
		attachments = new HashSet<>();
		logs = new HashSet<>();
	}
	
	public static Props props(ActorRef sender, ActorRef converter, ActorRef database, Set<AttachmentType> requestedAttachmentTypes) {
		return Props.create(DatasetInfoBuilder.class, sender, converter, database, requestedAttachmentTypes);
	}
	
	@Override
	public void preStart() throws Exception {
		metadataDocumentFactory = new MetadataDocumentFactory();
		
		getContext().setReceiveTimeout(Duration.create(15, TimeUnit.SECONDS));
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.error("timeout");
			
			sendUnavailable();
		} else if(msg instanceof MetadataNotFound) {
			log.debug("metadata not found");
			
			tellTarget(new DatasetNotFound(((MetadataNotFound) msg).getIdentification()));
		} else if(msg instanceof MetadataItem) {
			log.debug("metadata item");
			
			handleMetadataItem((MetadataItem)msg);
		} if(msg instanceof TableNotFound) {
			log.debug("table not found");
			
			logs.add(Log.create(LogLevel.ERROR, DatasetLogType.TABLE_NOT_FOUND, new DatabaseLog(tableName)));
			sendUnavailable();
		} else if(msg instanceof TableInfo) {
			log.debug("table info");
			
			tableInfo = (TableInfo)msg;
			sendResponse();
		} else if(msg instanceof Long) {
			log.debug("number of records");
			
			numberOfRecords = (Long)msg;
			sendResponse();
		} else {
			unhandled(msg);
		}
	}
	
	private void tellTarget(Object msg) {
		sender.tell(msg, converter);
		getContext().stop(getSelf());
	}
	
	private void sendUnavailable() {
		tellTarget(new UnavailableDatasetInfo(identification, reportedTitle, alternateTitle, categoryId, revisionDate, attachments, logs));		
	}
	
	private void sendResponse() {
		if(tableInfo != null && numberOfRecords != null) {
			tellTarget(new VectorDatasetInfo(identification, reportedTitle, alternateTitle, categoryId, revisionDate, attachments, logs, tableName, tableInfo, numberOfRecords));
		}
	}
	
	private void handleMetadataItem(MetadataItem metadataItem) {
		identification = metadataItem.getIdentification();
		byte[] content = metadataItem.getContent();
		
		if(requestedAttachmentTypes.contains(AttachmentType.METADATA)) {
			attachments.add(new Attachment(identification, AttachmentType.METADATA, content));
		}
		
		try {
			MetadataDocument metadataDocument = metadataDocumentFactory.parseDocument(content);
			
			try {
				title = metadataDocument.getTitle();				
			} catch(NotFound nf) {
				addMetadataParsingError(MetadataField.TITLE, MetadataLogType.NOT_FOUND, null);
			}
			
			try {
				alternateTitle = metadataDocument.getAlternateTitle();				
			} catch(NotFound nf) {
				addMetadataParsingError(MetadataField.ALTERNATE_TITLE, MetadataLogType.NOT_FOUND, null);				
			}
			
			try {
				revisionDate = metadataDocument.getRevisionDate();				
			} catch(NotFound nf) {
				addMetadataParsingError(MetadataField.REVISION_DATE, MetadataLogType.NOT_FOUND, null);				
			}
			
			if(logs.isEmpty()) {
				tableName = ProviderUtils.getTableName(alternateTitle);
				
				categoryId = ProviderUtils.getCategoryId(tableName);
				
				if(title == null) {
					reportedTitle = alternateTitle;
				} else {
					reportedTitle = title;
				}
				
				if(tableName != null && !tableName.trim().isEmpty()) {
					database.tell(new DescribeTable(tableName), getSelf());
					database.tell(new PerformCount(tableName), getSelf());
				} else {
					logs.add(Log.create(LogLevel.ERROR, DatasetLogType.UNKNOWN_TABLE));					
					sendUnavailable();
				}
			} else {
				sendUnavailable();
			}
		} catch(Exception e) {
			sendUnavailable();
		}
	}

	private void addMetadataParsingError(MetadataField field, MetadataLogType error, Object value) {		
		logs.add(Log.create(LogLevel.ERROR, DatasetLogType.METADATA_PARSING_ERROR, new MetadataLog(field, error, value)));
	}
}
