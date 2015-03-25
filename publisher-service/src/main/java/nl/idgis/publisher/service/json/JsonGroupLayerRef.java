package nl.idgis.publisher.service.json;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.GroupLayerRef;

public class JsonGroupLayerRef extends AbstractJsonLayerRef<GroupLayer> implements GroupLayerRef {

	public JsonGroupLayerRef(JsonNode jsonNode) {
		super(jsonNode, new JsonGroupLayer(jsonNode.get("layer")));
	}
	
	@Override
	public boolean isGroupRef() {
		return true;
	}

	@Override
	public GroupLayerRef asGroupRef() {
		return this;
	}		
}
