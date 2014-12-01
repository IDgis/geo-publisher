package nl.idgis.publisher.provider;

import java.util.HashSet;
import java.util.Set;

import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import akka.util.Timeout;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.StreamConverter;
import nl.idgis.publisher.stream.messages.Item;

public class MetadataDatasetInfoConverter extends StreamConverter {
	
	private static final Timeout timeout = Timeout.apply(15000);
	
	private final Set<AttachmentType> attachmentTypes;
	
	private final ActorRef database;
	
	private MetadataDocumentFactory metadataDocumentFactory;

	public MetadataDatasetInfoConverter(Set<AttachmentType> attachmentTypes, ActorRef target, ActorRef database) {
		super(target);
		
		this.attachmentTypes = attachmentTypes;
		this.database = database;
	}
	
	public static Props props(Set<AttachmentType> attachmentTypes, ActorRef target, ActorRef database) {
		return Props.create(MetadataDatasetInfoConverter.class, attachmentTypes, target, database);
	}
	
	@Override
	public void preStart() throws Exception {
		metadataDocumentFactory = new MetadataDocumentFactory();
	}

	@Override
	protected Future<Item> convert(Item item) throws Exception {
		if(item instanceof MetadataItem) {
			final MetadataItem metadataItem = (MetadataItem)item;
			
			final MetadataDocument metadataDocument = metadataDocumentFactory.parseDocument(metadataItem.getContent());			
			
			final String alternateTitle = metadataDocument.getAlternateTitle();
			final String tableName = getTableName(alternateTitle);
			final String categoryId = getCategoryId(tableName);
			
			final Future<Object> tableDescription = Patterns.ask(database, new DescribeTable(tableName), timeout);
			final Future<Object> numberOfRecords = Patterns.ask(database, new PerformCount(tableName), timeout);
			
			return tableDescription.flatMap(new Mapper<Object, Future<Item>>() {
				
				@Override
				public Future<Item> checkedApply(final Object tableDescription) {
					return numberOfRecords.map(new Mapper<Object, Item>() {
						
						@Override
						public Item checkedApply(final Object numberOfRecords) throws Exception {
							Set<Attachment> attachments = new HashSet<>();
							
							if(attachmentTypes.contains(AttachmentType.METADATA)) {
								// TODO: metadata document id?
								attachments.add(new Attachment(metadataItem.getIdentification(), AttachmentType.METADATA, metadataItem.getContent()));
							}
							
							return new VectorDatasetInfo(metadataItem.getIdentification(), metadataDocument.getTitle(), attachments, (TableDescription)tableDescription, (Long)numberOfRecords);
						}
						
					}, getContext().dispatcher());
				}
				
			}, getContext().dispatcher());
		} else {
			return Futures.successful(item);
		} 
	}
	
	private String getTableName(String alternateTitle) {
		if(alternateTitle != null 
			&& !alternateTitle.trim().isEmpty()) {			 
		
			final String tableName;
			if(alternateTitle.contains(" ")) {
				tableName = alternateTitle.substring(0, alternateTitle.indexOf(" ")).trim();
			} else {
				tableName = alternateTitle.trim();
			}
			
			return tableName.replace(":", ".").toLowerCase();
		}
		
		return null;
	}
	
	private String getCategoryId(String tableName) {
		if(tableName != null) {
			int separator = tableName.indexOf(".");
			if(separator != -1) {
				String schemaName = tableName.substring(0, separator);
				return schemaName.toLowerCase();
			}
		}
		
		return null;
	}

}
