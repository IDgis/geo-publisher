package nl.idgis.publisher.provider;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.harvest.MetadataField;
import nl.idgis.publisher.domain.job.harvest.MetadataLogType;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.domain.service.MetadataLog;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.metadata.messages.MetadataItem;
import nl.idgis.publisher.provider.metadata.messages.MetadataNotFound;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.DatasetNotFound;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.xml.exceptions.NotFound;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class AbstractDatasetInfoBuilder extends UntypedActor {

	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final ActorRef sender, converter;
	
	protected final Set<AttachmentType> requestedAttachmentTypes;
	
	protected Set<Attachment> attachments;
	
	protected Set<Log> logs;
	
	protected MetadataDocumentFactory metadataDocumentFactory;
	
	protected String identification, title, alternateTitle, reportedTitle, categoryId;
	
	protected Date revisionDate;
	
	protected AbstractDatasetInfoBuilder(ActorRef sender, ActorRef converter, Set<AttachmentType> requestedAttachmentTypes) {
		this.sender = sender;		
		this.converter = converter;		
		this.requestedAttachmentTypes = requestedAttachmentTypes;
	}
	
	@Override
	public final void preStart () throws Exception {
		attachments = new HashSet<>();
		logs = new HashSet<>();
		
		metadataDocumentFactory = new MetadataDocumentFactory();
		
		getContext().setReceiveTimeout(Duration.create(15, TimeUnit.SECONDS));
	}
	
	protected void onReceiveElse(Object msg) throws Exception {
		unhandled(msg);
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
		}  else {
			onReceiveElse(msg);
		}
	}
	
	protected void tellTarget(Object msg) {
		log.debug("result: {}", msg);
		
		sender.tell(msg, converter);
		getContext().stop(getSelf());
	}
	
	protected void sendUnavailable() {
		tellTarget(new UnavailableDatasetInfo(identification, reportedTitle, alternateTitle, categoryId, revisionDate, attachments, logs));		
	}
	
	protected abstract void processMetadata();
		
	private void handleMetadataItem(MetadataItem metadataItem) {
		identification = metadataItem.getIdentification();
		byte[] content = metadataItem.getContent();
		
		if(requestedAttachmentTypes.contains(AttachmentType.METADATA)) {
			attachments.add(new Attachment(identification, AttachmentType.METADATA, content));
		}
		
		try {
			log.debug("parsing metadata");
			MetadataDocument metadataDocument = metadataDocumentFactory.parseDocument(content);
			
			try {
				title = metadataDocument.getTitle();
				log.debug("title: {}", title);
			} catch(NotFound nf) {
				addMetadataParsingError(MetadataField.TITLE, MetadataLogType.NOT_FOUND, null);
			}
			
			try {
				alternateTitle = metadataDocument.getAlternateTitle();
				log.debug("alternateTitle: {}", alternateTitle);
			} catch(NotFound nf) {
				addMetadataParsingError(MetadataField.ALTERNATE_TITLE, MetadataLogType.NOT_FOUND, null);				
			}
			
			try {
				revisionDate = metadataDocument.getRevisionDate();
				log.debug("revisionDate: {}", revisionDate);
			} catch(NotFound nf) {
				addMetadataParsingError(MetadataField.REVISION_DATE, MetadataLogType.NOT_FOUND, null);				
			}
			
			processMetadata();
		} catch(Exception e) {
			sendUnavailable();
		}
	}
	
	private void addMetadataParsingError(MetadataField field, MetadataLogType error, Object value) {		
		logs.add(Log.create(LogLevel.ERROR, DatasetLogType.METADATA_PARSING_ERROR, new MetadataLog(field, error, value)));
	}
}
