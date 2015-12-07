package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;
import java.util.Optional;

import akka.actor.ActorRef;

public class EnsureTarget implements Serializable {

	private static final long serialVersionUID = 309822508209301465L;

	private final ActorRef actorRef;
	
	private final String environmentId;
	
	public EnsureTarget(ActorRef actorRef) {
		this(actorRef, null);
	}
	
	public EnsureTarget(ActorRef actorRef, String environmentId) {
		this.actorRef = actorRef;
		this.environmentId = environmentId;
	}
	
	public ActorRef getActorRef() {
		return actorRef;
	}
	
	public Optional<String> getEnvironmentId() {
		return Optional.ofNullable(environmentId);
	}
}
