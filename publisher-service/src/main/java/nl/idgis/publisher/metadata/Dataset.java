package nl.idgis.publisher.metadata;

import java.io.Serializable;

public class Dataset implements Serializable {

	private static final long serialVersionUID = -5027365177246967333L;
	
	private final String uuid, fileUuid, layerName;
	
	public Dataset(String uuid, String fileUuid, String layerName) {
		this.uuid = uuid;
		this.fileUuid = fileUuid;
		this.layerName = layerName;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getFileUuid() {
		return fileUuid;
	}
	
	public String getLayerName() {
		return layerName;
	}
}
