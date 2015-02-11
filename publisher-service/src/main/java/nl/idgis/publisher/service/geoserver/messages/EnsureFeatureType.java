package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;

public class EnsureFeatureType implements Serializable {

	private static final long serialVersionUID = 8855565860385703053L;
	
	private final String featureTypeId, tableName;
	
	public EnsureFeatureType(String featureTypeId, String tableName) {		
		this.featureTypeId = featureTypeId;
		this.tableName = tableName;
	}	

	public String getFeatureTypeId() {
		return featureTypeId;
	}
	
	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "EnsureFeatureType [featureTypeId=" + featureTypeId
				+ ", tableName=" + tableName + "]";
	}
}
