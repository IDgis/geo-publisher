package nl.idgis.publisher.schemas;

public enum SchemaRef {

	GEOSERVER_SLD_1_0_0("/schemas/geoserver/sld/StyledLayerDescriptor.xsd"),
	GEOSERVER_SLD_1_1_0("/StyledLayerDescriptor-1.1.0.xsd");
	
	private final String path;
	
	SchemaRef(String path) {
		this.path = path;
	}
	
	String getPath() {
		return path;
	}
}
