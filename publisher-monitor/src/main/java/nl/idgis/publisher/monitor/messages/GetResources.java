package nl.idgis.publisher.monitor.messages;

import java.io.Serializable;

public class GetResources implements Serializable {
	
	private static final long serialVersionUID = -9100571135234784647L;
	
	private final Object resourceType;
	
	public GetResources(Object resourceType) {
		this.resourceType = resourceType;
	}
	
	public Object getResourceType() {
		return resourceType;
	}

	@Override
	public String toString() {
		return "GetResources [resourceType=" + resourceType + "]";
	}
}
