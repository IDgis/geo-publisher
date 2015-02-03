package nl.idgis.publisher.monitor.messages;

import java.io.Serializable;

public class ResourceDestroyed extends ResourceRef implements Serializable {
	
	private static final long serialVersionUID = -7154877887260455714L;

	public ResourceDestroyed(Object resourceType, Object resource) {
		super(resourceType, resource);		
	}

	@Override
	public String toString() {
		return "ResourceDestroyed [resourceType=" + resourceType
				+ ", resource=" + resource + "]";
	}
	
}
