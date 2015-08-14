package nl.idgis.publisher.metadata;

import java.util.Objects;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.metadata.messages.PutServiceMetadata;
import nl.idgis.publisher.metadata.messages.ServiceInfo;

public class ServiceMetadataGenerator extends AbstractMetadataItemGenerator<ServiceInfo,PutServiceMetadata> {

	public ServiceMetadataGenerator(ActorRef metadataTarget, ServiceInfo serviceInfo) {
		super(metadataTarget, serviceInfo);
	}
	
	public static Props props(ActorRef metadataTarget, ServiceInfo serviceInfo) {
		return Props.create(
			ServiceMetadataGenerator.class, 
			Objects.requireNonNull(metadataTarget, "metadataTarget must not be null"), 
			Objects.requireNonNull(serviceInfo, "serviceInfo must not be null"));
	}

	@Override
	protected PutServiceMetadata generateMetadata(MetadataDocument metadataDocument) {
		return new PutServiceMetadata(itemInfo.getId(), metadataDocument);
	}

}
