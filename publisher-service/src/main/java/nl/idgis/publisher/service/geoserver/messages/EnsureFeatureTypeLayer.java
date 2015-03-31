package nl.idgis.publisher.service.geoserver.messages;

import java.util.List;
import java.util.stream.Collectors;

import nl.idgis.publisher.domain.web.tree.Tiling;

import nl.idgis.publisher.service.geoserver.rest.Attribute;
import nl.idgis.publisher.service.geoserver.rest.FeatureType;
import nl.idgis.publisher.service.geoserver.rest.Layer;
import nl.idgis.publisher.service.geoserver.rest.StyleRef;

public class EnsureFeatureTypeLayer extends EnsureLayer {

	private static final long serialVersionUID = 4489704653307367970L;

	private final String tableName;
	
	private final List<String> columnNames;
	
	private final List<String> keywords;
	
	private final String defaultStyleName, groupStyleName;
	
	private final List<String> additionalStyleNames;
	
	public EnsureFeatureTypeLayer(String layerId, String title, String abstr, List<String> keywords, String tableName, 
			List<String> columnNames, Tiling tilingSettings, String defaultStyleName, String groupStyleName, 
			List<String> additionalStyleNames) {
		super(layerId, title, abstr, tilingSettings);
		
		this.tableName = tableName;
		this.columnNames = columnNames;
		this.keywords = keywords;
		this.defaultStyleName = defaultStyleName;
		this.groupStyleName = groupStyleName;
		this.additionalStyleNames = additionalStyleNames;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public List<String> getColumnNames() {
		return columnNames;
	}
	
	public List<String> getKeywords() {
		return keywords;
	}
	
	public String getDefaultStyleName() {
		return defaultStyleName;
	}
	
	public String getGroupStyleName() {
		return groupStyleName;
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
				keywords,
				columnNames.stream()
					.map(Attribute::new)
					.collect(Collectors.toList()));
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
		return "EnsureFeatureTypeLayer [tableName=" + tableName
				+ ", columnNames=" + columnNames + ", keywords=" + keywords
				+ ", defaultStyleName=" + defaultStyleName
				+ ", groupStyleName=" + groupStyleName
				+ ", additionalStyleNames=" + additionalStyleNames + "]";
	}
		
}
