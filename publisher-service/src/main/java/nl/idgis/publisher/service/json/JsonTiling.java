package nl.idgis.publisher.service.json;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.Tiling;

import static nl.idgis.publisher.service.json.JsonService.getStream;
import static java.util.stream.Collectors.toList;

public class JsonTiling implements Tiling {
	
	private final JsonNode jsonNode;
	
	public JsonTiling(JsonNode jsonNode) {
		this.jsonNode = jsonNode;
	}

	@Override
	public List<String> getMimeFormats() {
		return 
			getStream(jsonNode, "mimeFormats")
				.map(JsonNode::asText)
				.collect(toList());
	}

	@Override
	public Integer getMetaWidth() {
		return jsonNode.get("metaWidth").asInt();
	}

	@Override
	public Integer getMetaHeight() {
		return jsonNode.get("metaHeight").asInt();
	}

	@Override
	public Integer getExpireCache() {
		return jsonNode.get("expireCache").asInt();
	}

	@Override
	public Integer getExpireClients() {
		return jsonNode.get("expireClients").asInt();
	}

	@Override
	public Integer getGutter() {
		return jsonNode.get("gutter").asInt();
	}

}
