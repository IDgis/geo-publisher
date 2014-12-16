package nl.idgis.publisher.harvester.sources;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;
import nl.idgis.publisher.harvester.sources.messages.ListDatasets;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.GetDatasetInfo;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class ProviderDataSource extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final Set<AttachmentType> metadataType;
		
	private final ActorRef provider;
	
	public ProviderDataSource(ActorRef provider) {		
		this.provider = provider;
		
		metadataType = new HashSet<>();
		metadataType.add(AttachmentType.METADATA);
	}
	
	public static Props props(ActorRef provider) {
		return Props.create(ProviderDataSource.class, provider);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ListDatasets) {
			handleListDatasets((ListDatasets)msg);
		} else if(msg instanceof GetDatasetMetadata) {
			handleGetDatasetMetadata((GetDatasetMetadata)msg);
		} else if(msg instanceof GetDataset) {
			handleGetDataset((GetDataset)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private void handleListDatasets(ListDatasets msg) {
		log.debug("retrieving datasets from provider");
		
		ActorRef converter = getContext().actorOf(
				ProviderDatasetConverter.props(provider),
				nameGenerator.getName(ProviderDatasetConverter.class));
		
		converter.forward(msg, getContext());
	}
	
	private void handleGetDataset(final GetDataset msg) {
		log.debug("retrieving data from provider");
		
		Props receiverProps = msg.getReceiverProps();
		ActorRef receiver = getContext().actorOf(
				receiverProps, 
				nameGenerator.getName(receiverProps.clazz()));
		
		ActorRef initiator = getContext().actorOf(
				ProviderGetDatasetInitiater.props(getSender(), msg, receiver, provider),
				nameGenerator.getName(ProviderGetDatasetInitiater.class));
		
		provider.tell(new GetDatasetInfo(Collections.<AttachmentType>emptySet(), msg.getId()), initiator);
	}
	
	private void handleGetDatasetMetadata(GetDatasetMetadata msg) {				
		log.debug("retrieving dataset metadata from provider");
		
		ActorRef builder = getContext().actorOf(
				ProviderMetadataDocumentBuilder.props(getSender()),
				nameGenerator.getName(ProviderMetadataDocumentBuilder.class));
		
		provider.tell(new GetDatasetInfo(metadataType, msg.getDatasetId()), builder);
	}

}
