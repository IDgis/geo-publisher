package nl.idgis.publisher.provider;

import java.util.Set;

import nl.idgis.publisher.provider.metadata.messages.GetAllMetadata;
import nl.idgis.publisher.provider.metadata.messages.MetadataItem;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.stream.StreamConverter;
import nl.idgis.publisher.stream.messages.Start;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import akka.actor.ActorRef;
import akka.actor.Props;

public class DatasetInfoConverter extends StreamConverter {
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final Set<AttachmentType> requestedAttachmentTypes;
	
	private final ActorRef metadata;
	
	private final DatasetInfoBuilderPropsFactory datasetInfoBuilderPropsFactory;

	public DatasetInfoConverter(Set<AttachmentType> requestedAttachmentTypes, ActorRef metadata, DatasetInfoBuilderPropsFactory datasetInfoBuilderPropsFactory) {		
		this.requestedAttachmentTypes = requestedAttachmentTypes;
		this.metadata = metadata;
		this.datasetInfoBuilderPropsFactory = datasetInfoBuilderPropsFactory;
	}
	
	public static Props props(Set<AttachmentType> attachmentTypes, ActorRef metadata, DatasetInfoBuilderPropsFactory datasetInfoBuilderPropsFactory) {
		return Props.create(DatasetInfoConverter.class, attachmentTypes, metadata, datasetInfoBuilderPropsFactory);
	}

	@Override
	protected boolean convert(Object msg, ActorRef sender) {
		if(msg instanceof MetadataItem) {
			log.debug("converting metadata item to dataset info");
			
			Props datasetInfoBuilderProps = datasetInfoBuilderPropsFactory.props(getSelf(), requestedAttachmentTypes);
			ActorRef datasetInfoBuilder = getContext().actorOf(datasetInfoBuilderProps, nameGenerator.getName(VectorDatasetInfoBuilder.class));
			datasetInfoBuilder.tell(msg, getSelf());
			
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void start(Start msg) throws Exception {
		if(msg instanceof ListDatasetInfo) {
			metadata.tell(new GetAllMetadata(), getSelf());
		} else {
			unhandled(msg);
		}
	}
}
