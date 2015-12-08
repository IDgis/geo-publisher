package nl.idgis.publisher.service.json;

import static java.util.stream.Collectors.toList;
import static nl.idgis.publisher.service.json.JsonService.getStream;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.RasterDatasetLayer;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.domain.web.tree.VectorDatasetLayer;

public abstract class AbstractJsonDatasetLayer extends AbstractJsonLayer implements DatasetLayer {
	
	private final String metadataFileIdentification;
	
	public AbstractJsonDatasetLayer(JsonNode jsonNode, Optional<String> metadataFileIdentification) {
		super(jsonNode);
		
		this.metadataFileIdentification = metadataFileIdentification.orElse (null);
	}
	
	@Override
	public Optional<String> getMetadataFileIdentification () {
		return Optional.ofNullable (metadataFileIdentification);
	}
	
	@Override
	public List<String> getKeywords() {
		return 
			getStream(jsonNode, "keywords")
				.map(JsonNode::asText)
				.collect(toList());
	}	

	@Override
	public List<StyleRef> getStyleRefs() {
		return 
			getStream(jsonNode, "styleRefs")
				.map(JsonStyleRef::new)
				.collect(toList());
	}	
	
	@Override
	public boolean isVectorLayer() {
		return false;
	}

	@Override
	public VectorDatasetLayer asVectorLayer() {
		throw new IllegalStateException("DatasetLayer is not a VectorDatasetLayer");
	}
	
	@Override
	public boolean isRasterLayer() {
		return false;
	}
	
	@Override
	public RasterDatasetLayer asRasterLayer() {
		throw new IllegalStateException("DatasetLayer is not a RasterDatasetLayer");
	}
	
	static DatasetLayer fromJson(JsonNode jsonNode, Map<String, Optional<String>> metadataFileIdentifications) {
		String layerName = Objects.requireNonNull(jsonNode.get("name"), 
			"layer name attribute missing").asText();
		String layerType = Objects.requireNonNull(jsonNode.get("type"), 
			"layer type attribute missing").asText();
		
		if(!metadataFileIdentifications.containsKey(layerName)) {
			throw new IllegalArgumentException("no metadata file identification for layer: " + layerName);
		}
		
		switch(layerType) {
			case "vector":
				return new JsonVectorDatasetLayer(jsonNode, metadataFileIdentifications.get(layerName));
			case "raster":
				return new JsonRasterDatasetLayer(jsonNode, metadataFileIdentifications.get(layerName));
			default:
				throw new IllegalArgumentException("unknown layer type: " + layerType);
		}
	}	
}
