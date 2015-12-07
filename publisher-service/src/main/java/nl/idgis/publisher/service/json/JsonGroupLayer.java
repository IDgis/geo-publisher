package nl.idgis.publisher.service.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;

import static java.util.stream.Collectors.toList;
import static nl.idgis.publisher.service.json.JsonService.getStream;

public class JsonGroupLayer extends AbstractJsonLayer implements GroupLayer {
	
	private final Map<String, String> datasetIds;
	
	public JsonGroupLayer(JsonNode jsonNode, Map<String, String> datasetIds) {
		super(jsonNode);
		
		this.datasetIds = datasetIds;
	}	

	@Override
	public List<LayerRef<? extends Layer>> getLayers() {
		return 
			getStream(jsonNode, "layers")
				.map(jsonNode -> AbstractJsonLayerRef.fromJson(jsonNode, datasetIds))
				.collect(toList());
	}
	
}
