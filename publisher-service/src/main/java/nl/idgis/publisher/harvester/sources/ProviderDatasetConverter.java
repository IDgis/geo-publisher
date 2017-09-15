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

import com.typesafe.config.Config;

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
import nl.idgis.publisher.utils.ConfigUtils;
import nl.idgis.publisher.xml.exceptions.NotFound;

public class ProviderDatasetConverter extends StreamConverter {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Set<AttachmentType> attachmentTypes;
	
	private final ActorRef provider; 
	
	private final MetadataDocumentFactory mdf;
	
	private final String confidentialPath;
	
	private final String dataPublicValue;
	
	private final String metadataPublicValue;
	
	private final String wmsOnlyValue;
	
	public ProviderDatasetConverter(ActorRef provider, Config harvesterConfig) throws Exception {
		this.provider = provider;
		
		confidentialPath = ConfigUtils.getOptionalString(harvesterConfig, "confidentialPath");
		dataPublicValue = ConfigUtils.getOptionalString(harvesterConfig, "dataPublicValue");
		metadataPublicValue = ConfigUtils.getOptionalString(harvesterConfig, "metadataPublicValue");
		wmsOnlyValue = ConfigUtils.getOptionalString(harvesterConfig,"wmsOnlyValue");
		
		log.debug("confidentialPath: {}", confidentialPath);
		log.debug("dataPublicValue: {}", dataPublicValue);
		log.debug("metadataPublicValue: {}", metadataPublicValue);
		log.debug("wmsOnlyValue: {}", wmsOnlyValue);
		
		Set<AttachmentType> attachmentTypes = new HashSet<>();
		attachmentTypes.add(AttachmentType.METADATA);
		attachmentTypes.add(AttachmentType.PHYSICAL_NAME);
		this.attachmentTypes = Collections.unmodifiableSet(attachmentTypes);
		
		mdf = new MetadataDocumentFactory();
	}
	
	public static Props props(ActorRef provider, Config harvesterConfig) {
		return Props.create(ProviderDatasetConverter.class, provider, harvesterConfig);
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
			
			log.debug("Converting provider dataset info for dataset {}", identification);
			
			Map<AttachmentType, Attachment> attachments = datasetInfo.getAttachments().stream()
				.collect(Collectors.toMap(
					Attachment::getAttachmentType,
					Function.identity()));
			
			MetadataDocument metadata;
			
			String physicalName = null;
			if(attachments.containsKey(AttachmentType.PHYSICAL_NAME)) {
				Attachment attachment = attachments.get(AttachmentType.PHYSICAL_NAME);
				if(attachment.getContent() instanceof String) {
					physicalName = (String) attachment.getContent();
				}
			}
			
			String refreshFrequency = null;
			boolean confidential = true;
			boolean metadataConfidential = true;
			boolean wmsOnly = false;
			
			if(attachments.containsKey(AttachmentType.METADATA)) {
				Attachment attachment = attachments.get(AttachmentType.METADATA);
				Object content = attachment.getContent();
				if(content instanceof byte[]) {
					try {
						metadata = mdf.parseDocument((byte[])attachment.getContent());
						
						try {
							refreshFrequency = metadata.getMaintenanceFrequencyCodeListValue();
							log.debug("import frequency code list value: {}", refreshFrequency);
						} catch(NotFound nf) {
							log.debug("import frequency code list value not found");
						}
						
						try {
							if(confidentialPath != null) {
								List<String> useLimitations = metadata.getMetadataElements(confidentialPath);
								
								log.debug("useLimitations: {}", useLimitations);
								
								if(dataPublicValue != null) {
									confidential = !useLimitations.contains(dataPublicValue);
									log.debug("data confidential: {}", confidential);
								}
								
								if(metadataPublicValue != null) {
									metadataConfidential = !useLimitations.contains(metadataPublicValue);
									log.debug("metadata confidential: {}", metadataConfidential);
								}
								
								if(wmsOnlyValue != null) {
									wmsOnly = useLimitations.contains(wmsOnlyValue);
									log.debug("wmsOnly: {}", wmsOnly);
								}
							} 
						} catch(Exception e) {
							log.error(e, "Couldn't process metadata");
						}
					} catch(Exception e) {
						log.error(e, "Couldn't parse metadata");
						metadata = null;
					}
				} else {
					log.warning("Unexpected metadata object: {}", content);
					metadata = null;
				}
			} else {
				log.warning("No metadata received");
				metadata = null;
			}
			
			final Dataset dataset;
			if(datasetInfo instanceof VectorDatasetInfo) {
				log.debug("vector dataset info: {}", datasetInfo);
				
				VectorDatasetInfo vectorDatasetInfo = (VectorDatasetInfo)datasetInfo;
				TableInfo tableInfo = vectorDatasetInfo.getTableInfo();
				
				List<Column> columns = Arrays.stream(tableInfo.getColumns())
					.map(column -> new Column(
						column.getName(), 
						column.getType(), 
						column.getAlias().orElse(null)))
					.collect(Collectors.toList());
				
				Table table = new Table(columns);
				dataset = new VectorDataset(identification, title, alternateTitle, categoryId, revisionDate, logs, confidential, metadataConfidential, wmsOnly, metadata, table, physicalName, refreshFrequency);
			} else if(msg instanceof RasterDatasetInfo) {
				log.debug("raster dataset info type");
				
				dataset = new RasterDataset(identification, title, alternateTitle, categoryId, revisionDate, logs, confidential, metadataConfidential, wmsOnly, metadata, physicalName, refreshFrequency);
			} else {
				log.debug("unhandled dataset info type");
				
				dataset = new UnavailableDataset(identification, title, alternateTitle, categoryId, revisionDate, logs, confidential, metadataConfidential, wmsOnly, metadata, physicalName, refreshFrequency);
			}
			
			log.debug("resulting dataset: {}", dataset);
			
			getSelf().tell(dataset, getSelf());
			return true;
		} else {
			return false;
		}
	}

	
}
