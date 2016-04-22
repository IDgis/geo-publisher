package nl.idgis.dav.model;

public class DefaultResourceDescription implements ResourceDescription {
	
	private final String name;
	
	private final ResourceProperties properties;
	
	public DefaultResourceDescription(String name, ResourceProperties properties) {
		this.name = name;
		this.properties = properties;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public ResourceProperties properties() {		
		return properties;
	}

}