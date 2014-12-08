package nl.idgis.publisher.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.harvest.MetadataField;
import nl.idgis.publisher.domain.job.harvest.MetadataLogType;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
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
	
	private final ActorRef sender, cursor, database;
	
	private final Set<AttachmentType> requestedAttachmentTypes;
	
	private final Set<Attachment> attachments;
	
	private final Set<Log> logs;
	
	private MetadataDocumentFactory metadataDocumentFactory;
	
	private String identification;
	
	private String title, alternateTitle, tableName, reportedTitle, categoryId;
	
	private Date revisionDate;
	
	private TableDescription tableDescription;
	
	private List<MetadataLogType> errors = new ArrayList<>();
	
	private List<MetadataField> fields = new ArrayList<>();					
	
	private List<Object> values = new ArrayList<>();
	
	private Long numberOfRecords;

	public DatasetInfoBuilder(ActorRef sender, ActorRef cursor, ActorRef database, Set<AttachmentType> requestedAttachmentTypes) {			
		this.sender = sender;		
		this.cursor = cursor;
		this.database = database;
		this.requestedAttachmentTypes = requestedAttachmentTypes;
		
		attachments = new HashSet<>();
		logs = new HashSet<>();
	}
	
	public static Props props(ActorRef sender, ActorRef cursor, ActorRef database, Set<AttachmentType> requestedAttachmentTypes) {
		return Props.create(DatasetInfoBuilder.class, sender, cursor, database, requestedAttachmentTypes);
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
			
			getContext().stop(getSelf());
		} else if(msg instanceof MetadataItem) {
			log.debug("metadata item");
			
			handleMetadataItem((MetadataItem)msg);
		} if(msg instanceof TableNotFound) {
			log.debug("table not found");
			
			sendUnavailable();
		} else if(msg instanceof TableDescription) {
			log.debug("table description");
			
			tableDescription = (TableDescription)msg;
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
		sender.tell(msg, cursor);
		getContext().stop(getSelf());
	}
	
	private void sendUnavailable() {
		tellTarget(new UnavailableDatasetInfo(identification, reportedTitle, attachments, logs));		
	}
	
	private void sendResponse() {
		if(tableDescription != null && numberOfRecords != null) {
			tellTarget(new VectorDatasetInfo(identification, reportedTitle, attachments, logs, tableDescription, numberOfRecords));
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
				errors.add(MetadataLogType.NOT_FOUND);
				fields.add(MetadataField.TITLE);						
				values.add(null);
			}
			
			try {
				alternateTitle = metadataDocument.getAlternateTitle();				
			} catch(NotFound nf) {
				errors.add(MetadataLogType.NOT_FOUND);
				fields.add(MetadataField.ALTERNATE_TITLE);						
				values.add(null);
			}
			
			try {
				revisionDate = metadataDocument.getRevisionDate();				
			} catch(NotFound nf) {
				errors.add(MetadataLogType.NOT_FOUND);
				fields.add(MetadataField.REVISION_DATE);						
				values.add(null);
			}
			
			Iterator<MetadataLogType> errorsItr = errors.iterator();
			Iterator<MetadataField> fieldsItr = fields.iterator();
			Iterator<Object> valuesItr = values.iterator();
			
			for(;errorsItr.hasNext();) {
				MetadataLogType error = errorsItr.next();
				MetadataField field = fieldsItr.next();
				Object value = valuesItr.next();
			}
			
			tableName = ProviderUtils.getTableName(alternateTitle);
			
			categoryId = ProviderUtils.getCategoryId(tableName);
			
			if(title == null) {
				reportedTitle = alternateTitle;
			} else {
				reportedTitle = title;
			}
			
			database.tell(new DescribeTable(tableName), getSelf());
			database.tell(new PerformCount(tableName), getSelf());
		} catch(Exception e) {
			sendUnavailable();
		}
	}
}
