package nl.idgis.publisher.metadata;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.metadata.messages.DatasetRef;
import nl.idgis.publisher.metadata.messages.PutServiceMetadata;
import nl.idgis.publisher.metadata.messages.ServiceInfo;
import nl.idgis.publisher.xml.exceptions.NotFound;

public class ServiceMetadataGenerator extends AbstractMetadataItemGenerator<ServiceInfo,PutServiceMetadata> {
	
	private static final String ENDPOINT_CODE_LIST_VALUE = "WebServices";

	private static final String ENDPOINT_CODE_LIST = "http://www.isotc211.org/2005/iso19119/resources/Codelist/gmxCodelists.xml#DCPList";

	private static final String ENDPOINT_OPERATION_NAME = "GetCapabilitities";

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
	protected List<PutServiceMetadata> generateMetadata(MetadataDocument metadataDocument) throws Exception {
		// TODO handle different environments
		for(String environmentId : itemInfo.getEnvironmentIds()) {
			log.debug("environmentId: {}", environmentId);
		}
		
		metadataDocument.removeOperatesOn();
		
		String href = "http://host/metadata/dataset/";
		// TODO fetch href from config
		
		for(DatasetRef datasetRef : itemInfo.getDatasetRefs()) {
			String uuid = datasetRef.getUuid();
			String fileUuid = datasetRef.getFileUuid();
			String uuidref = href + fileUuid + ".xml";
			
			log.debug("service operatesOn uuidref: {}, uuid: {}", uuidref, uuid);
			
			metadataDocument.addOperatesOn(uuid, uuidref);
		}
		
		metadataDocument.removeServiceType();
		metadataDocument.removeServiceEndpoint();			
		metadataDocument.removeBrowseGraphic();
		metadataDocument.removeServiceLinkage();
		metadataDocument.removeSVCoupledResource();
		
		return Arrays.asList(
			new PutServiceMetadata(itemInfo.getId() + "-wms", generateWMSMetadata(metadataDocument.clone())),
			new PutServiceMetadata(itemInfo.getId() + "-wfs", generateWFSMetadata(metadataDocument.clone())));
	}

	private MetadataDocument generateWFSMetadata(MetadataDocument metadataDocument) throws NotFound {
		log.debug("wfs metadata");
		
		String linkage = itemInfo.getId();
		// TODO: compute proper linkage
		
		metadataDocument.addServiceType("OGC:WFS");
		metadataDocument.addServiceEndpoint(ENDPOINT_OPERATION_NAME, ENDPOINT_CODE_LIST, ENDPOINT_CODE_LIST_VALUE, linkage);
		
		for(DatasetRef datasetRef : itemInfo.getDatasetRefs()) {
			String uuid = datasetRef.getUuid();
			for(String layerName : datasetRef.getLayerNames()) {
				String scopedName = layerName;
				
				log.debug("dataset scopedName: {}, uuid: {}, layerName: {}", scopedName, uuid, layerName);
				
				metadataDocument.addServiceLinkage(linkage, "OGC:WFS", layerName);
				metadataDocument.addSVCoupledResource("GetFeature", uuid, scopedName); 
			}
		}
		
		return metadataDocument;
	}

	private MetadataDocument generateWMSMetadata(MetadataDocument metadataDocument) throws NotFound {
		log.debug("wms metadata");
		
		String linkage = itemInfo.getId();
		// TODO: compute proper linkage
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
				metadataDocument.addServiceLinkage(linkage, "OGC:WMS", layerName);
				metadataDocument.addSVCoupledResource("GetMap", uuid, scopedName); 
			}
		}
		
		return metadataDocument;
	}

}
