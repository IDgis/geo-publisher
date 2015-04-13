package nl.idgis.publisher.service.json;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.StyleRef;

public class JsonDatasetLayerRef extends AbstractJsonLayerRef<DatasetLayer> implements DatasetLayerRef {
	
	public JsonDatasetLayerRef(JsonNode jsonNode) {
		super(jsonNode, AbstractJsonDatasetLayer.fromJson(jsonNode.get("layer")));
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
	public StyleRef getStyleRef() {
		return jsonNode.has ("styleRef") ? new JsonStyleRef(jsonNode.get("styleRef")) : null;		
	}

}
