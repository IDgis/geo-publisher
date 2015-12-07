package nl.idgis.publisher.service.json;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.GroupLayerRef;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;

public abstract class AbstractJsonLayerRef<T extends Layer> implements LayerRef<T> {
	
	protected final JsonNode jsonNode;

	protected final T layer;
	
	AbstractJsonLayerRef(JsonNode jsonNode, T layer) {
		this.jsonNode = jsonNode;
		this.layer = layer;		
	}

	@Override
	public T getLayer() {
		return layer;
	}	

	@Override
	public GroupLayerRef asGroupRef() {
		throw new IllegalStateException("LayerRef is not a GroupLayerRef");
	}

	@Override
	public DatasetLayerRef asDatasetRef() {
		throw new IllegalStateException("LayerRef is not a DatasetLayerRef");
	}
	
	static LayerRef<? extends Layer> fromJson(JsonNode jsonNode, Map<String, String> datasetIds) {
		if(jsonNode.get("groupRef").asBoolean()) {
			return new JsonGroupLayerRef(jsonNode, datasetIds);
		} else {
			return new JsonDatasetLayerRef(jsonNode, datasetIds);
		}
	}
}
