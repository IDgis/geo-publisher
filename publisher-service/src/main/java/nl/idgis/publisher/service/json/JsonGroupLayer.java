package nl.idgis.publisher.service.json;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;

import static java.util.stream.Collectors.toList;
import static nl.idgis.publisher.service.json.JsonService.getStream;

public class JsonGroupLayer extends AbstractJsonLayer implements GroupLayer {	
	
	public JsonGroupLayer(JsonNode jsonNode) {
		super(jsonNode);
	}	

	@Override
	public List<LayerRef<? extends Layer>> getLayers() {
		return 
			getStream(jsonNode, "layers")
				.map(AbstractJsonLayerRef::fromJson)
				.collect(toList());
	}
	
}
