package nl.idgis.publisher.metadata;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.metadata.messages.DatasetInfo;
import nl.idgis.publisher.metadata.messages.PutDatasetMetadata;
import nl.idgis.publisher.metadata.messages.ServiceRef;

public class DatasetMetadataGenerator extends AbstractMetadataItemGenerator<DatasetInfo, PutDatasetMetadata> {

	public DatasetMetadataGenerator(ActorRef metadataTarget, DatasetInfo datasetInfo, String serviceLinkagePrefix) {
		super(metadataTarget, datasetInfo, serviceLinkagePrefix);
	}
	
	public static Props props(ActorRef metadataTarget, DatasetInfo datasetInfo, String serviceLinkagePrefix) {
		return Props.create(
			DatasetMetadataGenerator.class, 
			Objects.requireNonNull(metadataTarget, "metadataTarget must not be null"), 
			Objects.requireNonNull(datasetInfo, "datasetInfo must not be null"),
			Objects.requireNonNull(serviceLinkagePrefix, "serviceLinkagePrefix must not be null"));
	}

	@Override
	protected List<PutDatasetMetadata> generateMetadata(MetadataDocument metadataDocument) throws Exception {
		metadataDocument.setDatasetIdentifier(itemInfo.getDatasetUuid());
		metadataDocument.setFileIdentifier(itemInfo.getFileUuid());
		
		metadataDocument.removeServiceLinkage();
		for(ServiceRef serviceRef : itemInfo.getServiceRefs()) {
			for(String layerName : serviceRef.getLayerNames()) {
				for(ServiceType serviceType : ServiceType.values()) {
					String linkage = getServiceLinkage(serviceRef.getServiceId(), serviceType);
					String protocol = serviceType.getProtocol();
					
					log.debug("service linkage: {}, protocol: {}, layerName: {}", linkage, protocol, layerName);
					metadataDocument.addServiceLinkage(linkage, protocol, layerName);
				}
			}
		};
		
		return Collections.singletonList(new PutDatasetMetadata(itemInfo.getId(), metadataDocument));
	}

}
