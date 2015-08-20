package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class GenerateMetadata implements Serializable {

	private static final long serialVersionUID = 5879974272274728002L;

	private final String environmentId;
	
	private final ActorRef target;
	
	public GenerateMetadata(String environmentId, ActorRef target) {
		this.environmentId = environmentId;
		this.target = target;
	}

	public String getEnvironmentId() {
		return environmentId;
	}

	public ActorRef getTarget() {
		return target;
	}

	@Override
	public String toString() {
		return "GenerateMetadata [environmentId=" + environmentId + ", target=" + target + "]";
	}	
}
