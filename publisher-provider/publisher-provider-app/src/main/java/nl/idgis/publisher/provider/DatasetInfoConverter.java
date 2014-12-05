package nl.idgis.publisher.provider;

import java.util.Set;

import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.StreamConverter;
import nl.idgis.publisher.stream.messages.Item;

import akka.actor.ActorRef;
import akka.actor.Props;

public class DatasetInfoConverter extends StreamConverter<DatasetInfo> {
	
	private final Set<AttachmentType> requestedAttachmentTypes;
	
	private final ActorRef target, database;

	public DatasetInfoConverter(Set<AttachmentType> requestedAttachmentTypes, ActorRef target, ActorRef database) {
		super(target);
		
		this.requestedAttachmentTypes = requestedAttachmentTypes;
		this.target = target;
		this.database = database;
	}
	
	public static Props props(Set<AttachmentType> attachmentTypes, ActorRef target, ActorRef database) {
		return Props.create(DatasetInfoConverter.class, attachmentTypes, target, database);
	}

	@Override
	protected void convert(Item item) {
		if(item instanceof MetadataItem) {
			Props datasetInfoBuilderProps = DatasetInfoBuilder.props(target, getSender(), database, requestedAttachmentTypes);
			ActorRef datasetInfoBuilder = getContext().actorOf(datasetInfoBuilderProps);
			datasetInfoBuilder.forward(item, getContext());
		} else {
			unhandled(item);
		}
	}
}
