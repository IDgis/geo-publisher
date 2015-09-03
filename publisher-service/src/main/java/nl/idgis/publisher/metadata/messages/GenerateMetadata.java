package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

import nl.idgis.publisher.metadata.MetadataGenerator;

/**
 * Request {@link MetadataGenerator} to begin updating metadata 
 * for a specific environment.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class GenerateMetadata implements Serializable {	

	private static final long serialVersionUID = -2990168366163441674L;

	private final String environmentId;
	
	private final ActorRef target;
	
	private final String serviceLinkagePrefix, datasetMetadataPrefix;	
	
	public GenerateMetadata(String environmentId, ActorRef target, String serviceLinkagePrefix, String datasetMetadataPrefix) {
		this.environmentId = environmentId;
		this.target = target;
		this.serviceLinkagePrefix = serviceLinkagePrefix;
		this.datasetMetadataPrefix = datasetMetadataPrefix;
	}

	public String getEnvironmentId() {
		return environmentId;
	}

	public ActorRef getTarget() {
		return target;
	}
	
	public String getServiceLinkagePrefix() {
		return serviceLinkagePrefix;
	}
	
	public String getDatasetMetadataPrefix() {		
		return datasetMetadataPrefix;
	}

	@Override
	public String toString() {
		return "GenerateMetadata [environmentId=" + environmentId + ", target=" + target + ", serviceLinkagePrefix="
				+ serviceLinkagePrefix + ", datasetMetadataPrefix=" + datasetMetadataPrefix + "]";
	}
			
}
