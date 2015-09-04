package nl.idgis.publisher.metadata;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.metadata.messages.DatasetRef;
import nl.idgis.publisher.metadata.messages.KeepMetadata;
import nl.idgis.publisher.metadata.messages.MetadataType;
import nl.idgis.publisher.metadata.messages.UpdateMetadata;
import nl.idgis.publisher.metadata.messages.ServiceInfo;
import nl.idgis.publisher.xml.exceptions.NotFound;

/**
 * This actor generates service metadata documents.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class ServiceMetadataGenerator extends AbstractMetadataItemGenerator<ServiceInfo> {
	
	private static final String ENDPOINT_CODE_LIST_VALUE = "WebServices";

	private static final String ENDPOINT_CODE_LIST = "http://www.isotc211.org/2005/iso19119/resources/Codelist/gmxCodelists.xml#DCPList";

	private static final String ENDPOINT_OPERATION_NAME = "GetCapabilitities";

	public ServiceMetadataGenerator(ActorRef metadataTarget, ServiceInfo serviceInfo, String serviceLinkagePrefix, String datasetMetadataPrefix) {
		super(metadataTarget, serviceInfo, serviceLinkagePrefix, datasetMetadataPrefix);
	}
	
	/**
	 * Creates a {@link Props} for the {@link ServiceMetadataGenerator} actor.
	 * 
	 * @param metadataTarget a reference to the metadata target actor.
	 * @param serviceInfo the object containing information about the service. 
	 * @param serviceLinkagePrefix the service linkage url prefix.
	 * @param datasetMetadataPrefix the dataset url prefix.
	 * @return
	 */
	public static Props props(ActorRef metadataTarget, ServiceInfo serviceInfo, String serviceLinkagePrefix, String datasetMetadataPrefix) {
		return Props.create(
			ServiceMetadataGenerator.class, 
			Objects.requireNonNull(metadataTarget, "metadataTarget must not be null"), 
			Objects.requireNonNull(serviceInfo, "serviceInfo must not be null"),
			Objects.requireNonNull(serviceLinkagePrefix, "serviceLinkagePrefix must not be null"),
			Objects.requireNonNull(datasetMetadataPrefix, "datasetMetadataPrefix must not be null"));
	}
	
	@Override
	protected List<UpdateMetadata> updateMetadata(MetadataDocument metadataDocument) throws Exception {
		metadataDocument.removeOperatesOn();		
		
		for(DatasetRef datasetRef : itemInfo.getDatasetRefs()) {
			String uuid = datasetRef.getUuid();
			String fileUuid = datasetRef.getFileUuid();
			String uuidref = getDatasetMetadataHref(fileUuid);
			
			log.debug("service operatesOn uuidref: {}, uuid: {}", uuidref, uuid);
			
			metadataDocument.addOperatesOn(uuid, uuidref);
		}
		
		metadataDocument.removeServiceType();
		metadataDocument.removeServiceEndpoint();			
		metadataDocument.removeBrowseGraphic();
		metadataDocument.removeServiceLinkage();
		metadataDocument.removeSVCoupledResource();
		
		String serviceId = itemInfo.getId();
		
		return Arrays.asList(
			new UpdateMetadata(MetadataType.SERVICE, serviceId + "-wms", generateWMSMetadata(metadataDocument.clone())),
			new UpdateMetadata(MetadataType.SERVICE, serviceId + "-wfs", generateWFSMetadata(metadataDocument.clone())));
	}

	/**
	 * Creates a WFS service metadata document.
	 * 
	 * @param metadataDocument the source document.
	 * @return the resulting WFS service metadata document.
	 * @throws NotFound
	 */
	private MetadataDocument generateWFSMetadata(MetadataDocument metadataDocument) throws NotFound {
		log.debug("wfs metadata");
		
		ServiceType serviceType = ServiceType.WFS;
		String linkage = getServiceLinkage(itemInfo.getName(), serviceType);
		
		metadataDocument.addServiceType("OGC:WFS");
		metadataDocument.addServiceEndpoint(ENDPOINT_OPERATION_NAME, ENDPOINT_CODE_LIST, ENDPOINT_CODE_LIST_VALUE, linkage);
		
		for(DatasetRef datasetRef : itemInfo.getDatasetRefs()) {
			String uuid = datasetRef.getUuid();
			for(String layerName : datasetRef.getLayerNames()) {
				String scopedName = layerName;
				
				log.debug("dataset scopedName: {}, uuid: {}, layerName: {}", scopedName, uuid, layerName);
				
				metadataDocument.addServiceLinkage(linkage, serviceType.getProtocol(), layerName);
				metadataDocument.addSVCoupledResource("GetFeature", uuid, scopedName); 
			}
		}
		
		return metadataDocument;
	}

	/**
	 * Creates a WMS service metadata document.
	 * 
	 * @param metadataDocument the source document.
	 * @return the resulting WMS service metadata document.
	 * @throws NotFound
	 */
	private MetadataDocument generateWMSMetadata(MetadataDocument metadataDocument) throws NotFound {
		log.debug("wms metadata");		
		
		ServiceType serviceType = ServiceType.WMS;
		String linkage = getServiceLinkage(itemInfo.getName(), serviceType);
		
		String browseGraphicBaseUrl = linkage 
			+ "request=GetMap&Service=WMS&SRS=EPSG:28992&CRS=EPSG:28992"
			+ "&Bbox=180000,459000,270000,540000&Width=600&Height=662&Format=image/png&Styles=";
		
		metadataDocument.addServiceType("OGC:WMS");
		metadataDocument.addServiceEndpoint(ENDPOINT_OPERATION_NAME, ENDPOINT_CODE_LIST, ENDPOINT_CODE_LIST_VALUE, linkage);
		
		for(DatasetRef datasetRef : itemInfo.getDatasetRefs()) {
			String uuid = datasetRef.getUuid();
			for(String layerName : datasetRef.getLayerNames()) {
				String scopedName = layerName;
				
				log.debug("dataset scopedName: {}, uuid: {}, layerName: {}", scopedName, uuid, layerName);
				
				metadataDocument.addBrowseGraphic(browseGraphicBaseUrl + "&layers=" + layerName);
				metadataDocument.addServiceLinkage(linkage, serviceType.getProtocol(), layerName);
				metadataDocument.addSVCoupledResource("GetMap", uuid, scopedName); 
			}
		}
		
		return metadataDocument;
	}

	@Override
	protected List<KeepMetadata> keepMetadata() {
		String serviceId = itemInfo.getId();
		
		return Arrays.asList(
			new KeepMetadata(MetadataType.SERVICE, serviceId + "-wms"),
			new KeepMetadata(MetadataType.SERVICE, serviceId + "-wfs"));
	}

}
