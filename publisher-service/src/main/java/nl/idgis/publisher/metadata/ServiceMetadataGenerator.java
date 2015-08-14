package nl.idgis.publisher.metadata;

import akka.actor.Props;

import nl.idgis.publisher.metadata.messages.ServiceInfo;

public class ServiceMetadataGenerator extends AbstractMetadataItemGenerator<ServiceInfo> {

	public ServiceMetadataGenerator(ServiceInfo serviceInfo) {
		super(serviceInfo);
	}
	
	public static Props props(ServiceInfo serviceInfo) {
		return Props.create(ServiceMetadataGenerator.class, serviceInfo);
	}

	@Override
	protected void generateMetadata(MetadataDocument metadataDocument) {
		
	}

}
