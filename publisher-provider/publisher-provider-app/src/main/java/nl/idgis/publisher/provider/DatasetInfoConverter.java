package nl.idgis.publisher.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.harvest.MetadataField;
import nl.idgis.publisher.domain.job.harvest.MetadataLogType;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.StreamConverter;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.xml.exceptions.NotFound;

import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class DatasetInfoConverter extends StreamConverter<DatasetInfo> {

	private static final Timeout timeout = Timeout.apply(15000);
	
	private final Set<AttachmentType> requestedAttachmentTypes;
	
	private final ActorRef database;
	
	private MetadataDocumentFactory metadataDocumentFactory;

	public DatasetInfoConverter(Set<AttachmentType> requestedAttachmentTypes, ActorRef target, ActorRef database) {
		super(target);
		
		this.requestedAttachmentTypes = requestedAttachmentTypes;
		this.database = database;
	}
	
	public static Props props(Set<AttachmentType> attachmentTypes, ActorRef target, ActorRef database) {
		return Props.create(DatasetInfoConverter.class, attachmentTypes, target, database);
	}
	
	@Override
	public void preStart() throws Exception {
		metadataDocumentFactory = new MetadataDocumentFactory();
	}
	
	private <T> Future<T> askDatabaseFlatMap(Object msg, Mapper<Object, Future<T>> mapper) {
		return Patterns.ask(database, msg, timeout).flatMap(mapper, getContext().dispatcher());
	}
	
	private <T> Future<T> askDatabaseMap(Object msg, Mapper<Object, T> mapper) {
		return Patterns.ask(database, msg, timeout).map(mapper, getContext().dispatcher());
	}
	
	private class DatasetInfoBuilder {
		private final Set<Attachment> attachments;
		
		private final Set<Log> logs;
		
		private final String identification;
		
		private String title, alternateTitle, tableName, reportedTitle, categoryId;
		
		private Date revisionDate;
		
		private TableDescription tableDescription;
		
		private List<MetadataLogType> errors = new ArrayList<>();
		
		private List<MetadataField> fields = new ArrayList<>();					
		
		private List<Object> values = new ArrayList<>();
		
		public DatasetInfoBuilder(String identification, byte[] content) {			
			this.identification = identification;
			
			attachments = new HashSet<>();
			if(requestedAttachmentTypes.contains(AttachmentType.METADATA)) {
				attachments.add(new Attachment(identification, AttachmentType.METADATA, content));
			}
			
			logs = new HashSet<>();
		}
		
		private class PerformCountMapper extends Mapper<Object, DatasetInfo> {
			
			@Override
			public DatasetInfo checkedApply(Object msg) throws IllegalArgumentException {
				if(msg instanceof Long) {
					long numberOfRecords = (Long)msg;
					return new VectorDatasetInfo(identification, title, attachments, logs, tableDescription, numberOfRecords);
				} else {
					throw new IllegalArgumentException("Long expected");
				}
			}
		}
		
		private class DescribeTableMapper extends Mapper<Object, Future<DatasetInfo>> {
			
			@Override
			public Future<DatasetInfo> checkedApply(Object msg) throws IllegalArgumentException {
				if(msg instanceof TableNotFound) {
					return getUnavailable();
				} else if(msg instanceof TableDescription) {
					tableDescription = (TableDescription)msg;
					return askDatabaseMap(new PerformCount(tableName), new PerformCountMapper());
				} else {
					throw new IllegalArgumentException("TableNotFound or TableDescription expected");
				}
			}
		}
		
		private Future<DatasetInfo> forMetadataDocument(MetadataDocument metadataDocument) {		
			processMetadata(metadataDocument);
			
			if(title == null) {
				reportedTitle = alternateTitle;
			} else {
				reportedTitle = title;
			}
			
			if(tableName != null) {			
				return askDatabaseFlatMap(new DescribeTable(tableName), new DescribeTableMapper());
			} else {
				return getUnavailable();
			}
		}
		
		private Future<DatasetInfo> getUnavailable() {
			return Futures.<DatasetInfo>successful(new UnavailableDatasetInfo(identification, reportedTitle, attachments, logs));
		}

		private void processMetadata(MetadataDocument metadataDocument) {
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
					
			if(alternateTitle.contains(" ")) {
				tableName = alternateTitle.substring(0, alternateTitle.indexOf(" ")).trim();
			} else {
				tableName = alternateTitle.trim();
			}
			
			tableName = tableName.replace(":", ".").toLowerCase();		
			
			int separator = tableName.indexOf(".");
			if(separator != -1) {
				String schemaName = tableName.substring(0, separator);
				categoryId = schemaName.toLowerCase();
			} else {
				
			}
		}
	}

	@Override
	protected Future<DatasetInfo> convert(Item item) {
		if(item instanceof MetadataItem) {
			MetadataItem metadataItem = (MetadataItem)item;
			
			String identification = metadataItem.getIdentification();			
			byte[] content = metadataItem.getContent();
			
			DatasetInfoBuilder builder = new DatasetInfoBuilder(identification, content);
			
			try {
				MetadataDocument metadataDocument = metadataDocumentFactory.parseDocument(content);
				return builder.forMetadataDocument(metadataDocument);
			} catch(Exception e) {
				return builder.getUnavailable();
			}
		} else {
			return Futures.failed(new IllegalArgumentException("MetadataItem expected"));
		}
	}
}
