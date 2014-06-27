package nl.idgis.publisher.monitor.messages;

public abstract class ResourceRef {

	private final Object resourceType, resource;
	
	public ResourceRef(Object resourceType, Object resource) {
		this.resourceType = resourceType;
		this.resource = resource;
	}
	
	public Object getResourceType() {
		return resourceType;
	}
	
	public Object getResource() {
		return resource;
	}
}
