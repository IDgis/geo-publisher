package nl.idgis.publisher.service.geoserver.messages;

import java.util.List;

import nl.idgis.publisher.domain.web.tree.Tiling;
import nl.idgis.publisher.service.geoserver.rest.Coverage;

public class EnsureCoverageLayer extends EnsureDatasetLayer {	
	
	private static final long serialVersionUID = 1744704363061599107L;
	
	private final String fileName;

	public EnsureCoverageLayer(String layerId, String title, String abstr, List<String> keywords, List<String> metadataLinks, boolean wmsOnly,
		Tiling tilingSettings, String defaultStyleName, String groupStyleName, List<String> additionalStyleNames, boolean reimported, String fileName) {
		super(layerId, title, abstr, keywords, metadataLinks, wmsOnly, tilingSettings, defaultStyleName, groupStyleName, additionalStyleNames, reimported);
		
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}
	
	public String getNativeName() {
		return fileName.split("\\.")[0];
	}
	
	public Coverage getCoverage() {
		return new Coverage(layerId, getNativeName(), title, abstr, keywords, getMetadataLinks());
	}
	
	@Override
	public String toString() {
		return "EnsureCoverageLayer [fileName=" + fileName + ", layerId="
				+ layerId + ", title=" + title + ", abstr=" + abstr
				+ ", tilingSettings=" + tilingSettings + "]";
	}
}
