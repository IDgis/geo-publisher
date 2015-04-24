package nl.idgis.publisher.provider;

import java.util.Set;

import nl.idgis.publisher.provider.protocol.AttachmentType;

import akka.actor.ActorRef;
import akka.actor.Props;

public interface DatasetInfoBuilderPropsFactory {

	Props props(ActorRef sender, Set<AttachmentType> requestedAttachmentTypes);
}
