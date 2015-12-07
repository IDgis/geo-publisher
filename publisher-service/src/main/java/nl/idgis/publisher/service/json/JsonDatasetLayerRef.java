package nl.idgis.publisher.service.json;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.StyleRef;

import static nl.idgis.publisher.service.json.JsonService.getOptional;

public class JsonDatasetLayerRef extends AbstractJsonLayerRef<DatasetLayer> implements DatasetLayerRef {
	
	public JsonDatasetLayerRef(JsonNode jsonNode, Map<String, String> datasetIds) {
		super(jsonNode, AbstractJsonDatasetLayer.fromJson(jsonNode.get("layer"), datasetIds));
	}

	@Override
	public boolean isGroupRef() {
		return false;
	}
	
	@Override
	public DatasetLayerRef asDatasetRef() {		
		return this;
	}

	@Override
	public Optional<StyleRef> getStyleRef() {
		return 
			getOptional(jsonNode, "styleRef")
				.map(JsonStyleRef::new);		
	}

}
