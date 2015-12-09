package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import akka.actor.ActorRef;

public class EnsureTarget implements Serializable {

	private static final long serialVersionUID = 309822508209301465L;

	private final ActorRef actorRef;
	
	private final EnvironmentInfo environmentInfo;
	
	public EnsureTarget(ActorRef actorRef) {
		this(actorRef, Optional.empty ());
	}
	
	public EnsureTarget(ActorRef actorRef, Optional<EnvironmentInfo> environmentInfo) {
		this.actorRef = actorRef;
		this.environmentInfo = Objects.requireNonNull (environmentInfo, "environmentInfo cannot be null").orElse (null);
	}
	
	public ActorRef getActorRef() {
		return actorRef;
	}

	public Optional<EnvironmentInfo> getEnvironmentInfo () {
		return Optional.ofNullable (environmentInfo);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actorRef == null) ? 0 : actorRef.hashCode());
		result = prime * result + ((environmentInfo == null) ? 0 : environmentInfo.hashCode());
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
		if (environmentInfo == null) {
			if (other.environmentInfo != null)
				return false;
		} else if (!environmentInfo.equals(other.environmentInfo))
			return false;
		return true;
	}

	public static class EnvironmentInfo implements Serializable {
		private static final long serialVersionUID = -5789468069618228782L;
		
		private final String environmentId;
		private final String metadataUrl;
		
		public EnvironmentInfo (final String environmentId, final String metadataUrl) {
			this.environmentId = Objects.requireNonNull (environmentId, "environmentId cannot be null");
			this.metadataUrl = Objects.requireNonNull(metadataUrl, "metadataUrl cannot be null"); 
		}

		public String getEnvironmentId () {
			return environmentId;
		}

		public String getMetadataUrl () {
			return metadataUrl;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((environmentId == null) ? 0 : environmentId.hashCode());
			result = prime * result + ((metadataUrl == null) ? 0 : metadataUrl.hashCode());
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
			EnvironmentInfo other = (EnvironmentInfo) obj;
			if (environmentId == null) {
				if (other.environmentId != null)
					return false;
			} else if (!environmentId.equals(other.environmentId))
				return false;
			if (metadataUrl == null) {
				if (other.metadataUrl != null)
					return false;
			} else if (!metadataUrl.equals(other.metadataUrl))
				return false;
			return true;
		}
	}
}
