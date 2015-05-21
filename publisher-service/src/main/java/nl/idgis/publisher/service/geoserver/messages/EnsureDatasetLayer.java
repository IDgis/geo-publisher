package nl.idgis.publisher.service.geoserver.messages;

import java.util.List;
import java.util.stream.Collectors;

import nl.idgis.publisher.domain.web.tree.Tiling;

import nl.idgis.publisher.service.geoserver.rest.Layer;
import nl.idgis.publisher.service.geoserver.rest.StyleRef;

public abstract class EnsureDatasetLayer extends EnsureLayer {	

	private static final long serialVersionUID = -4749381624211825293L;

	protected final String defaultStyleName, groupStyleName;
	
	protected final List<String> additionalStyleNames;
	
	protected final List<String> keywords;

	protected EnsureDatasetLayer(String layerId, String title, String abstr, List<String> keywords, Tiling tilingSettings,
		String defaultStyleName, String groupStyleName, List<String> additionalStyleNames, boolean reimported) {
		
		super(layerId, title, abstr, tilingSettings, reimported);
		
		this.keywords = keywords;
		this.defaultStyleName = defaultStyleName;
		this.groupStyleName = groupStyleName;
		this.additionalStyleNames = additionalStyleNames;
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

	public Layer getLayer() {
		return new Layer(
			layerId, 
			new StyleRef(defaultStyleName), 
			additionalStyleNames.stream()
				.map(styleName -> new StyleRef(styleName))
				.collect(Collectors.toList()));
	}
}
