package nl.idgis.publisher.service.geoserver.messages;

import java.util.List;

import nl.idgis.publisher.domain.web.tree.Tiling;

import nl.idgis.publisher.service.geoserver.rest.FeatureType;

public class EnsureFeatureTypeLayer extends EnsureLayer {

	private static final long serialVersionUID = 9083018890281059932L;

	private final String tableName;
	
	private final List<String> keywords;
	
	public EnsureFeatureTypeLayer(String layerId, String title, String abstr, List<String> keywords, String tableName, Tiling tilingSettings) {
		super(layerId, title, abstr, tilingSettings);
		
		this.tableName = tableName;
		this.keywords = keywords;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public List<String> getKeywords() {
		return keywords;
	}
	
	public FeatureType getFeatureType() {
		return new FeatureType(
				layerId, 
				tableName,
				title,
				abstr,
				keywords);
	}

	@Override
	public String toString() {
		return "EnsureFeatureTypeLayer [tableName=" + tableName + ", keywords="
				+ keywords + ", layerId=" + layerId + ", title=" + title
				+ ", abstr=" + abstr + ", tilingSettings=" + tilingSettings
				+ "]";
	}	
}
