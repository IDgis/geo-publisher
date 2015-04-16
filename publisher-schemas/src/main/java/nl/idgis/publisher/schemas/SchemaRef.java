package nl.idgis.publisher.schemas;

public enum SchemaRef {

	GEOSERVER_SLD("/schemas/geoserver/sld/StyledLayerDescriptor.xsd");
	
	private final String path;
	
	SchemaRef(String path) {
		this.path = path;
	}
	
	String getPath() {
		return path;
	}
}
