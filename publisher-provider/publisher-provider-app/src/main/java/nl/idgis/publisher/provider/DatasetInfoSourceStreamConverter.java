package nl.idgis.publisher.provider;

import java.util.Set;

import nl.idgis.publisher.provider.metadata.messages.GetAllMetadata;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.stream.StreamConverter;
import nl.idgis.publisher.stream.messages.Start;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import akka.actor.ActorRef;
import akka.actor.Props;

public class DatasetInfoSourceStreamConverter extends StreamConverter {
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final Class<?> datasetInfoSourceType;
	
	private final Set<AttachmentType> requestedAttachmentTypes;
	
	private final ActorRef datasetInfoSource;
	
	private final DatasetInfoBuilderPropsFactory datasetInfoBuilderPropsFactory;

	public DatasetInfoSourceStreamConverter(Class<?> datasetInfoSourceType, Set<AttachmentType> requestedAttachmentTypes, ActorRef datasetInfoSource, DatasetInfoBuilderPropsFactory datasetInfoBuilderPropsFactory) {		
		this.datasetInfoSourceType = datasetInfoSourceType;
		this.requestedAttachmentTypes = requestedAttachmentTypes;
		this.datasetInfoSource = datasetInfoSource;
		this.datasetInfoBuilderPropsFactory = datasetInfoBuilderPropsFactory;
	}
	
	public static Props props(Class<?> datasetInfoSourceType, Set<AttachmentType> attachmentTypes, ActorRef datasetInfoSource, DatasetInfoBuilderPropsFactory datasetInfoBuilderPropsFactory) {
		return Props.create(DatasetInfoSourceStreamConverter.class, datasetInfoSourceType, attachmentTypes, datasetInfoSource, datasetInfoBuilderPropsFactory);
	}

	@Override
	protected boolean convert(Object msg) {
		if(datasetInfoSourceType.isInstance(msg)) {
			log.debug("converting metadata item to dataset info");
			
			Props datasetInfoBuilderProps = datasetInfoBuilderPropsFactory.props(getSelf(), requestedAttachmentTypes);
			ActorRef datasetInfoBuilder = getContext().actorOf(datasetInfoBuilderProps, nameGenerator.getName(datasetInfoBuilderProps.actorClass()));
			datasetInfoBuilder.tell(msg, getSelf());
			
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void start(Start msg) throws Exception {
		if(msg instanceof ListDatasetInfo) {
			datasetInfoSource.tell(new GetAllMetadata(), getSelf());
		} else {
			unhandled(msg);
		}
	}
}
