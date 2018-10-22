package nl.idgis.publisher.service.geoserver.messages;

import java.util.List;
import java.util.stream.Collectors;

import nl.idgis.publisher.domain.web.tree.Tiling;
import nl.idgis.publisher.service.geoserver.rest.Layer;
import nl.idgis.publisher.service.geoserver.rest.MetadataLink;
import nl.idgis.publisher.service.geoserver.rest.StyleRef;

public abstract class EnsureDatasetLayer extends EnsureLayer {
	
	private static final long serialVersionUID = 3753307750991615374L;
	
	protected final String defaultStyleName, groupStyleName;
	
	protected final List<String> additionalStyleNames;
	
	protected final List<String> keywords;
	
	protected final List<String> metadataLinks;
	
	protected final boolean wmsOnly;

	protected EnsureDatasetLayer(String layerId, String title, String abstr, List<String> keywords, List<String> metadataLinks,
			boolean wmsOnly, Tiling tilingSettings, String defaultStyleName, String groupStyleName, List<String> additionalStyleNames,
		boolean reimported) {
		
		super(layerId, title, abstr, tilingSettings, reimported);
		
		this.keywords = keywords;
		this.metadataLinks = metadataLinks;
		this.defaultStyleName = defaultStyleName;
		this.groupStyleName = groupStyleName;
		this.additionalStyleNames = additionalStyleNames;
		this.wmsOnly = wmsOnly;
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
				.collect(Collectors.toList()),
			!wmsOnly);
	}
	
	public List<MetadataLink> getMetadataLinks() {
		return metadataLinks.stream()
			.map(metadataLink -> new MetadataLink("application/xml", "ISO19115:2003", metadataLink))
			.collect(Collectors.toList());
	}

	public boolean isWmsOnly() {
		return wmsOnly;
	}
	
}
