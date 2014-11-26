package nl.idgis.publisher.metadata;

import java.io.Serializable;

public class Dataset implements Serializable {

	private static final long serialVersionUID = 8401593426598988033L;
	
	private final String uuid, layerName;
	
	public Dataset(String uuid, String layerName) {
		this.uuid = uuid;
		this.layerName = layerName;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getLayerName() {
		return layerName;
	}
}
