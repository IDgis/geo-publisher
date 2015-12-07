package nl.idgis.publisher.service.json;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.RasterDatasetLayer;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.domain.web.tree.VectorDatasetLayer;

import static nl.idgis.publisher.service.json.JsonService.getStream;
import static java.util.stream.Collectors.toList;

public abstract class AbstractJsonDatasetLayer extends AbstractJsonLayer implements DatasetLayer {
	
	private final String datasetId;
	
	public AbstractJsonDatasetLayer(JsonNode jsonNode, String datasetId) {
		super(jsonNode);
		
		this.datasetId = datasetId;
	}
	
	@Override
	public String getDatasetId() {
		return datasetId;
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
	
	static DatasetLayer fromJson(JsonNode jsonNode, Map<String, String> datasetIds) {
		String layerName = Objects.requireNonNull(jsonNode.get("name"), 
			"layer name attribute missing").asText();
		String layerType = Objects.requireNonNull(jsonNode.get("type"), 
			"layer type attribute missing").asText();
		
		if(!datasetIds.containsKey(layerName)) {
			throw new IllegalArgumentException("no dataset id for layer: " + layerName);
		}
		
		switch(layerType) {
			case "vector":
				return new JsonVectorDatasetLayer(jsonNode, datasetIds.get(layerName));
			case "raster":
				return new JsonRasterDatasetLayer(jsonNode, datasetIds.get(layerName));
			default:
				throw new IllegalArgumentException("unknown layer type: " + layerType);
		}
	}	
}
