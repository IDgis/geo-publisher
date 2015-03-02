package nl.idgis.publisher.service.geoserver.messages;

import java.util.List;
import java.util.stream.Collectors;

import nl.idgis.publisher.domain.web.tree.Tiling;

import nl.idgis.publisher.service.geoserver.rest.FeatureType;
import nl.idgis.publisher.service.geoserver.rest.Layer;
import nl.idgis.publisher.service.geoserver.rest.StyleRef;

public class EnsureFeatureTypeLayer extends EnsureLayer {

	private static final long serialVersionUID = 9083018890281059932L;

	private final String tableName;
	
	private final List<String> keywords;
	
	private final String defaultStyleName;
	
	private final List<String> additionalStyleNames;
	
	public EnsureFeatureTypeLayer(String layerId, String title, String abstr, List<String> keywords, String tableName, 
			Tiling tilingSettings, String defaultStyleName, List<String> additionalStyleNames) {
		super(layerId, title, abstr, tilingSettings);
		
		this.tableName = tableName;
		this.keywords = keywords;
		this.defaultStyleName = defaultStyleName;
		this.additionalStyleNames = additionalStyleNames;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public List<String> getKeywords() {
		return keywords;
	}
	
	public String getDefaultStyleName() {
		return defaultStyleName;
	}
	
	public List<String> getAdditionalStyleNames() {
		return additionalStyleNames;
	}
	
	public FeatureType getFeatureType() {
		return new FeatureType(
				layerId, 
				tableName,
				title,
				abstr,
				keywords);
	}
	
	public Layer getLayer() {
		return new Layer(
				layerId, 
				new StyleRef(defaultStyleName), 
				additionalStyleNames.stream()
					.map(styleName -> new StyleRef(styleName))
					.collect(Collectors.toList()));
	}

	@Override
	public String toString() {
		return "EnsureFeatureTypeLayer [tableName=" + tableName + ", keywords="
				+ keywords + ", layerId=" + layerId + ", title=" + title
				+ ", abstr=" + abstr + ", tilingSettings=" + tilingSettings
				+ "]";
	}	
}
