package nl.idgis.publisher.service.json;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.Tiling;

import static java.util.stream.Collectors.toList;
import static nl.idgis.publisher.service.json.JsonService.getOptional;
import static nl.idgis.publisher.service.json.JsonService.getStream;

public class JsonGroupLayer implements GroupLayer {

	private final JsonNode jsonNode;
	
	public JsonGroupLayer(JsonNode jsonNode) {
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
	public List<LayerRef<? extends Layer>> getLayers() {
		return 
			getStream(jsonNode, "layers")
				.map(AbstractJsonLayerRef::fromJson)
				.collect(toList());
	}
	
	@Override
	public boolean isConfidential () {
		return jsonNode.path ("confidential").asBoolean ();
	}
}
