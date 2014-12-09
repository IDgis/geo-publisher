package nl.idgis.publisher.harvester.sources;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;
import nl.idgis.publisher.harvester.sources.messages.ListDatasets;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.GetDatasetInfo;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.utils.Ask;

public class ProviderDataSource extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final int GET_VECTOR_DATASET_MESSAGE_SIZE = 10;
		
	private final ActorRef provider;
	
	private MetadataDocumentFactory metadataDocumentFactory;
	
	public ProviderDataSource(ActorRef provider) {		
		this.provider = provider;		
	}
	
	public static Props props(ActorRef provider) {
		return Props.create(ProviderDataSource.class, provider);
	}
	
	@Override
	public void preStart() throws Exception {
		metadataDocumentFactory = new MetadataDocumentFactory();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ListDatasets) {
			handleListDatasets();
		} else if(msg instanceof GetDatasetMetadata) {
			handleGetDatasetMetadata((GetDatasetMetadata)msg);
		} else if(msg instanceof GetDataset) {
			handleGetDataset((GetDataset)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private void handleListDatasets() {
		log.debug("retrieving datasets from provider");
		
		ActorRef providerDataset = getContext().actorOf(ProviderDatasetInfo.props(getSender(), provider));		
		provider.tell(new ListDatasetInfo(Collections.<AttachmentType>emptySet()), providerDataset);
	}
	
	private void handleGetDataset(final GetDataset gd) {
		log.debug("retrieving data from provider");
	}
	
	private static <T> T getAttachment(Set<Attachment> attachments, AttachmentType attachmentType, Class<T> clazz) {
		for(Attachment attachment : attachments) {
			Object content = attachment.getContent();
			if(attachment.getAttachmentType().equals(attachmentType) && clazz.isInstance(content)) {
				return clazz.cast(content);
			}
		}
		
		return null;
	}
	
	private void handleGetDatasetMetadata(GetDatasetMetadata gdm) {				
		log.debug("retrieving dataset metadata from provider");
		
		final ActorRef sender = getSender();
						
		Set<AttachmentType> attachmentTypes = new HashSet<>();
		attachmentTypes.add(AttachmentType.METADATA);
		Ask.ask(getContext(), provider, new GetDatasetInfo(attachmentTypes, gdm.getDatasetId()), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {					
					DatasetInfo datasetInfo = (DatasetInfo)msg;
					
					byte[] content = getAttachment(datasetInfo.getAttachments(), AttachmentType.METADATA, byte[].class);
					if(content == null) {
						log.error("no metadata");
					} else {
						log.debug("metadata retrieved");
						MetadataDocument metadataDocument = metadataDocumentFactory.parseDocument(content);
						log.debug("metadata parsed");					
						
						sender.tell(metadataDocument, getSelf());
					}
				}
			}, getContext().dispatcher());	
	}

}
