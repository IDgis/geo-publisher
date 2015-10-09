package nl.idgis.publisher.metadata;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.metadata.messages.DatasetInfo;
import nl.idgis.publisher.metadata.messages.KeepMetadata;
import nl.idgis.publisher.metadata.messages.MetadataType;
import nl.idgis.publisher.metadata.messages.UpdateMetadata;
import nl.idgis.publisher.metadata.messages.ServiceRef;

/**
 * This actor generates dataset metadata documents.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class DatasetMetadataGenerator extends AbstractMetadataItemGenerator<DatasetInfo> {

	public DatasetMetadataGenerator(ActorRef metadataTarget, DatasetInfo datasetInfo, String serviceLinkagePrefix, String datasetMetadataPrefix) {
		super(metadataTarget, datasetInfo, serviceLinkagePrefix, datasetMetadataPrefix);
	}
	
	/**
	 * Creates a {@link Props} for the {@link DatasetMetadataGenerator} actor.
	 * 
	 * @param metadataTarget a reference to the metadata target actor.
	 * @param datasetInfo the object containing information about the dataset. 
	 * @param serviceLinkagePrefix the service linkage url prefix.
	 * @param datasetMetadataPrefix the dataset url prefix.
	 * @return
	 */
	public static Props props(ActorRef metadataTarget, DatasetInfo datasetInfo, String serviceLinkagePrefix, String datasetMetadataPrefix) {
		return Props.create(
			DatasetMetadataGenerator.class, 
			Objects.requireNonNull(metadataTarget, "metadataTarget must not be null"), 
			Objects.requireNonNull(datasetInfo, "datasetInfo must not be null"),
			Objects.requireNonNull(serviceLinkagePrefix, "serviceLinkagePrefix must not be null"),
			Objects.requireNonNull(datasetMetadataPrefix, "datasetMetadataPrefix must not be null"));
	}

	@Override
	protected List<UpdateMetadata> updateMetadata(MetadataDocument metadataDocument) throws Exception {
		String fileIdentification = itemInfo.getMetadataFileId();
		
		metadataDocument.setDatasetIdentifier(itemInfo.getMetadataId());
		metadataDocument.setFileIdentifier(fileIdentification);
		
		metadataDocument.removeServiceLinkage();
		for(ServiceRef serviceRef : itemInfo.getServiceRefs()) {
			for(String layerName : serviceRef.getLayerNames()) {
				for(ServiceType serviceType : ServiceType.values()) {
					String linkage = getServiceLinkage(serviceRef.getServiceName(), serviceType);
					String protocol = serviceType.getProtocol();
					
					log.debug("service linkage: {}, protocol: {}, layerName: {}", linkage, protocol, layerName);
					metadataDocument.addServiceLinkage(linkage, protocol, layerName);
				}
			}
		};
		
		return Collections.singletonList(new UpdateMetadata(MetadataType.DATASET, fileIdentification, metadataDocument));
	}

	@Override
	protected List<KeepMetadata> keepMetadata() {
		return Collections.singletonList(new KeepMetadata(MetadataType.DATASET, itemInfo.getMetadataFileId()));
	}

}
