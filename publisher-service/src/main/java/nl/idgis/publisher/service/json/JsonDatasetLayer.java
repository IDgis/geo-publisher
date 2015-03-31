package nl.idgis.publisher.service.json;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.domain.web.tree.Tiling;

import static nl.idgis.publisher.service.json.JsonService.getOptional;
import static nl.idgis.publisher.service.json.JsonService.getStream;
import static java.util.stream.Collectors.toList;

public class JsonDatasetLayer implements DatasetLayer {
	
	private final JsonNode jsonNode;
	
	public JsonDatasetLayer(JsonNode jsonNode) {
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
	public String getTableName() {
		return jsonNode.get("tableName").asText();
	}
	
	@Override
	public List<String> getColumnNames() {
		return 
			getStream(jsonNode, "columnNames")
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

}
