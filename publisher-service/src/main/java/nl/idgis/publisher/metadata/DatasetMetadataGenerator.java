package nl.idgis.publisher.metadata;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.metadata.messages.DatasetInfo;
import nl.idgis.publisher.metadata.messages.PutDatasetMetadata;
import nl.idgis.publisher.metadata.messages.ServiceRef;

public class DatasetMetadataGenerator extends AbstractMetadataItemGenerator<DatasetInfo, PutDatasetMetadata> {

	public DatasetMetadataGenerator(ActorRef metadataTarget, DatasetInfo datasetInfo) {
		super(metadataTarget, datasetInfo);
	}
	
	public static Props props(ActorRef metadataTarget, DatasetInfo datasetInfo) {
		return Props.create(
			DatasetMetadataGenerator.class, 
			Objects.requireNonNull(metadataTarget, "metadataTarget must not be null"), 
			Objects.requireNonNull(datasetInfo, "datasetInfo must not be null"));
	}

	@Override
	protected PutDatasetMetadata generateMetadata(MetadataDocument metadataDocument) throws Exception {
		metadataDocument.setDatasetIdentifier(itemInfo.getDatasetUuid());
		metadataDocument.setFileIdentifier(itemInfo.getFileUuid());
		
		metadataDocument.removeServiceLinkage();
		for(ServiceRef serviceRef : itemInfo.getServiceRefs()) {
			for(String layerName : serviceRef.getLayerNames()) {
				for(String protocol : Arrays.asList("OGC:WMS", "OGC:WFS")) {
					String linkage = serviceRef.getServiceId();
					// TODO: compute proper linkage
					metadataDocument.addServiceLinkage(linkage, protocol, layerName);
				}
			}
		};
		
		return new PutDatasetMetadata(itemInfo.getId(), metadataDocument);
	}

}
