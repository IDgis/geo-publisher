package nl.idgis.publisher.service.geoserver.messages;

import nl.idgis.publisher.domain.web.tree.TilingSettings;

import nl.idgis.publisher.service.geoserver.rest.FeatureType;

public class EnsureFeatureTypeLayer extends EnsureLayer {	

	private static final long serialVersionUID = 2712860254027704849L;
	
	private final String tableName;
	
	public EnsureFeatureTypeLayer(String layerId, String title, String abstr, String tableName, TilingSettings tilingSettings) {
		super(layerId, title, abstr, tilingSettings);
		
		this.tableName = tableName;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public FeatureType getFeatureType() {
		return new FeatureType(
				layerId, 
				tableName,
				title,
				abstr);
	}

	@Override
	public String toString() {
		return "EnsureFeatureTypeLayer [tableName=" + tableName + ", layerId="
				+ layerId + ", title=" + title + ", abstr=" + abstr
				+ ", tilingSettings=" + tilingSettings + "]";
	}
	
}
