package nl.idgis.publisher.service.json;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.RasterDatasetLayer;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.domain.web.tree.Tiling;
import nl.idgis.publisher.domain.web.tree.VectorDatasetLayer;

import static nl.idgis.publisher.service.json.JsonService.getOptional;
import static nl.idgis.publisher.service.json.JsonService.getStream;
import static java.util.stream.Collectors.toList;

public abstract class AbstractJsonDatasetLayer implements DatasetLayer {
	
	protected final JsonNode jsonNode;
	
	public AbstractJsonDatasetLayer(JsonNode jsonNode) {
		this.jsonNode = jsonNode;
	}

	@Override
	public String getId() {
		return jsonNode.get("id").asText();
	}

	@Override
	public String getName() {
		return jsonNode.get("name").asText();
	}

	@Override
	public String getTitle() {
		return jsonNode.get("title").asText();
	}

	@Override
	public String getAbstract() {
		return jsonNode.get("abstract").asText();
	}

	@Override
	public Optional<Tiling> getTiling() {
		return 
			getOptional(jsonNode, "tiling")
				.map(JsonTiling::new);
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
	
	static DatasetLayer fromJson(JsonNode jsonNode) {
		JsonNode type = jsonNode.get("type");		
		Objects.requireNonNull(type, "layer type attribute missing");		
		String layerType = type.asText();
		
		switch(layerType) {
			case "vector":
				return new JsonVectorDatasetLayer(jsonNode);
			case "raster":
				return new JsonRasterDatasetLayer(jsonNode);
			default:
				throw new IllegalArgumentException("unknown layer type: " + layerType);
		}
	}
}
