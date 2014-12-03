package nl.idgis.publisher.provider;

import java.util.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import nl.idgis.publisher.domain.job.harvest.MetadataField;
import nl.idgis.publisher.domain.job.harvest.MetadataLogType;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.Message;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.database.TableNotFound;
import nl.idgis.publisher.xml.exceptions.NotFound;

import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class DatasetInfoBuilder {
	
	private static final Timeout timeout = Timeout.apply(15000);
	
	private final ActorRef database;
	
	private final ExecutionContext executionContext; 
	
	private final Set<Attachment> attachments;
	
	private final Set<Message<?>> messages;
	
	private final String identification;
	
	private final MetadataDocument metadataDocument;

	private String title, alternateTitle, tableName, reportedTitle, categoryId;
	
	private Date revisionDate;
	
	private TableDescription tableDescription;
	
	private List<MetadataLogType> errors = new ArrayList<>();
	
	private List<MetadataField> fields = new ArrayList<>();					
	
	private List<Object> values = new ArrayList<>();
	
	public DatasetInfoBuilder(ActorRef database, ExecutionContext executionContext, String identification, Set<Attachment> attachments, Set<Message<?>> messages, MetadataDocument metadataDocument) {
		this.database = database;
		this.executionContext = executionContext;
		this.identification = identification;
		this.attachments = attachments;
		this.messages = messages;
		this.metadataDocument = metadataDocument;
	}
	
	public static Future<DatasetInfo> apply(ActorRef database, ExecutionContext executionContext, String identification, Set<Attachment> attachments, Set<Message<?>> messages, MetadataDocument metadataDocument) {
		DatasetInfoBuilder builder = new DatasetInfoBuilder(database, executionContext, identification, attachments, messages, metadataDocument); 
		return builder.apply();
	}
	
	private class PerformCountResponse extends Mapper<Object, DatasetInfo> {
		
		@Override
		public DatasetInfo checkedApply(Object msg) throws IllegalArgumentException {
			if(msg instanceof Long) {
				long numberOfRecords = (Long)msg;
				return new VectorDatasetInfo(identification, title, attachments, messages, tableDescription, numberOfRecords);
			} else {
				throw new IllegalArgumentException("Long expected");
			}
		}
	}
	
	private class DescribeTableResponse extends Mapper<Object, Future<DatasetInfo>> {
		
		@Override
		public Future<DatasetInfo> checkedApply(Object msg) throws IllegalArgumentException {
			if(msg instanceof TableNotFound) {
				return getUnavailable();
			} else if(msg instanceof TableDescription) {
				tableDescription = (TableDescription)msg;
				return askDatabaseMap(new PerformCount(tableName), new PerformCountResponse());
			} else {
				throw new IllegalArgumentException("TableNotFound or TableDescription expected");
			}
		}
	}
	
	private Future<DatasetInfo> apply() {		
		processMetadata(metadataDocument);
		
		if(title == null) {
			reportedTitle = alternateTitle;
		} else {
			reportedTitle = title;
		}
		
		if(tableName != null) {			
			return askDatabaseFlatMap(new DescribeTable(tableName), new DescribeTableResponse());
		} else {
			return getUnavailable();
		}
	}
	
	private <T> Future<T> askDatabaseFlatMap(Object msg, Mapper<Object, Future<T>> mapper) {
		return Patterns.ask(database, msg, timeout).flatMap(mapper, executionContext);
	}
	
	private <T> Future<T> askDatabaseMap(Object msg, Mapper<Object, T> mapper) {
		return Patterns.ask(database, msg, timeout).map(mapper, executionContext);
	}
	
	private Future<DatasetInfo> getUnavailable() {
		return Futures.<DatasetInfo>successful(new UnavailableDatasetInfo(identification, reportedTitle, attachments, messages));
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
		}
	}
}
