package nl.idgis.publisher.harvester.sources;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.RasterDataset;
import nl.idgis.publisher.domain.service.UnavailableDataset;
import nl.idgis.publisher.domain.service.VectorDataset;
import nl.idgis.publisher.domain.service.Table;

import nl.idgis.publisher.harvester.sources.messages.ListDatasets;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.provider.protocol.RasterDatasetInfo;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.stream.StreamConverter;
import nl.idgis.publisher.stream.messages.Start;

public class ProviderDatasetConverter extends StreamConverter {
	
	private static final String DATA_NOT_CONFIDENTIAL_CONSTRAINT_VALUE = "Downloadable data";
	
	private static final String METADATA_NOT_CONFIDENTIAL_CONSTRAINT_VALUE = "Geoportaal extern";
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Set<AttachmentType> attachmentTypes;
	
	private final ActorRef provider; 
	
	private final MetadataDocumentFactory mdf;
	
	public ProviderDatasetConverter(ActorRef provider) throws Exception {
		this.provider = provider;
		
		Set<AttachmentType> attachmentTypes = new HashSet<>();
		attachmentTypes.add(AttachmentType.METADATA);
		this.attachmentTypes = Collections.unmodifiableSet(attachmentTypes);
		
		mdf = new MetadataDocumentFactory();
	}
	
	public static Props props(ActorRef provider) {
		return Props.create(ProviderDatasetConverter.class, provider);
	}

	@Override
	protected void start(Start msg) throws Exception {
		if(msg instanceof ListDatasets) {
			provider.tell(new ListDatasetInfo(attachmentTypes), getSelf());
		} else {
			unhandled(msg);
		}
	}

	@Override
	protected boolean convert(Object msg) throws Exception {
		if(msg instanceof DatasetInfo) {
			DatasetInfo datasetInfo = (DatasetInfo)msg;
			
			String identification = datasetInfo.getIdentification();
			String title = datasetInfo.getTitle();
			String alternateTitle = datasetInfo.getAlternateTitle();
			String categoryId = datasetInfo.getCategoryId();			
			Date revisionDate = datasetInfo.getRevisionDate();
			Set<Log> logs = datasetInfo.getLogs();
			
			Map<AttachmentType, Attachment> attachments = datasetInfo.getAttachments().stream()
				.collect(Collectors.toMap(
					Attachment::getAttachmentType,
					Function.identity()));
			
			boolean confidential = false;
			boolean metadataConfidential = false;
			MetadataDocument metadata;
			if(attachments.containsKey(AttachmentType.METADATA)) {
				Attachment attachment = attachments.get(AttachmentType.METADATA);
				Object content = attachment.getContent();
				if(content instanceof byte[]) {
					try {
						metadata = mdf.parseDocument((byte[])attachment.getContent());
						List<String> useLimitations = metadata.getUseLimitations();
						
						confidential = !useLimitations.contains(DATA_NOT_CONFIDENTIAL_CONSTRAINT_VALUE);
						metadataConfidential = !useLimitations.contains(METADATA_NOT_CONFIDENTIAL_CONSTRAINT_VALUE);
						
						log.debug("data confidential: " + confidential);
						log.debug("metadata confidential: " + metadataConfidential);
					} catch(Exception e) {
						metadata = null;
					}
				} else {
					metadata = null;
				}
			} else {
				metadata = null;
			}
			
			final Dataset dataset;
			if(datasetInfo instanceof VectorDatasetInfo) {
				log.debug("vector dataset info: " + datasetInfo);
				
				VectorDatasetInfo vectorDatasetInfo = (VectorDatasetInfo)datasetInfo;
				TableInfo tableInfo = vectorDatasetInfo.getTableInfo();
				
				List<Column> columns = Arrays.stream(tableInfo.getColumns())
					.map(column -> new Column(
						column.getName(), 
						column.getType(), 
						column.getAlias().orElse(null)))
					.collect(Collectors.toList());
				
				Table table = new Table(columns);				
				dataset = new VectorDataset(identification, title, alternateTitle, categoryId, revisionDate, logs, confidential, metadataConfidential, metadata, table);
			} else if(msg instanceof RasterDatasetInfo) {
				log.debug("raster dataset info type");
				
				dataset = new RasterDataset(identification, title, alternateTitle, categoryId, revisionDate, logs, confidential, metadataConfidential, metadata);
			} else {
				log.debug("unhandled dataset info type");
				
				dataset = new UnavailableDataset(identification, title, alternateTitle, categoryId, revisionDate, logs, confidential, metadataConfidential, metadata);
			}
			
			log.debug("resulting dataset: {}", dataset);
			
			getSelf().tell(dataset, getSelf());
			return true;
		} else {
			return false;
		}
	}

	
}
