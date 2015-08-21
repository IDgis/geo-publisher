package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class GenerateMetadata implements Serializable {
	
	private static final long serialVersionUID = -928423371634898960L;

	private final String environmentId;
	
	private final ActorRef target;
	
	private final String serviceLinkagePrefix;
	
	public GenerateMetadata(String environmentId, ActorRef target, String serviceLinkagePrefix) {
		this.environmentId = environmentId;
		this.target = target;
		this.serviceLinkagePrefix = serviceLinkagePrefix;
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

	@Override
	public String toString() {
		return "GenerateMetadata [environmentId=" + environmentId + ", target=" + target + ", serviceLinkagePrefix="
				+ serviceLinkagePrefix + "]";
	}		
}
