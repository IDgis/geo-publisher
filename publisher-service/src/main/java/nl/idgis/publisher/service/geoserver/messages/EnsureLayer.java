package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;

public class EnsureLayer implements Serializable {
	
	private static final long serialVersionUID = 5623191719963046510L;
	
	private final String layerId, tableName;
	
	public EnsureLayer(String layerId, String tableName) {		
		this.layerId = layerId;
		this.tableName = tableName;
	}	

	public String getLayerId() {
		return layerId;
	}
	
	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "EnsureLayer [layerId=" + layerId
				+ ", tableName=" + tableName + "]";
	}
}
