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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actorRef == null) ? 0 : actorRef.hashCode());
		result = prime * result + ((environmentId == null) ? 0 : environmentId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EnsureTarget other = (EnsureTarget) obj;
		if (actorRef == null) {
			if (other.actorRef != null)
				return false;
		} else if (!actorRef.equals(other.actorRef))
			return false;
		if (environmentId == null) {
			if (other.environmentId != null)
				return false;
		} else if (!environmentId.equals(other.environmentId))
			return false;
		return true;
	}
}
