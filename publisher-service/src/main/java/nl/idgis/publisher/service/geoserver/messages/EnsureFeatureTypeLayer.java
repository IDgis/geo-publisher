package nl.idgis.publisher.service.geoserver.messages;

import java.util.List;
import java.util.stream.Collectors;

import nl.idgis.publisher.domain.web.tree.Tiling;
import nl.idgis.publisher.service.geoserver.rest.Attribute;
import nl.idgis.publisher.service.geoserver.rest.FeatureType;

public class EnsureFeatureTypeLayer extends EnsureDatasetLayer {
	
	private static final long serialVersionUID = 7194270547429548309L;
	
	private final String tableName;
	
	private final List<String> columnNames;
	
	public EnsureFeatureTypeLayer(String layerId, String title, String abstr, List<String> keywords, List<String> metadataLinks,
			boolean wmsOnly, Tiling tilingSettings, String defaultStyleName, String groupStyleName, List<String> additionalStyleNames,
			boolean reimported, String tableName, List<String> columnNames) {
		super(layerId, title, abstr, keywords, metadataLinks, wmsOnly, tilingSettings, defaultStyleName, groupStyleName, additionalStyleNames, reimported);
		
		this.tableName = tableName;
		this.columnNames = columnNames;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public List<String> getColumnNames() {
		return columnNames;
	}
	
	public FeatureType getFeatureType() {
		return new FeatureType(
				layerId, 
				tableName,
				title,
				abstr,
				keywords,
				getMetadataLinks(),
				columnNames.stream()
					.map(Attribute::new)
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
