package nl.idgis.publisher.service.json;

import static nl.idgis.publisher.service.json.JsonService.asTextWithDefault;
import static nl.idgis.publisher.service.json.JsonService.getOptional;

import java.sql.Timestamp;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.Tiling;

public abstract class AbstractJsonLayer implements Layer {

	protected final JsonNode jsonNode;
	
	public AbstractJsonLayer(JsonNode jsonNode) {
		this.jsonNode = jsonNode;
	}
	
	@Override
	public final String getId() {
		return jsonNode.get("id").asText();
	}

	@Override
	public final String getName() {
		return jsonNode.get("name").asText();
	}

	@Override
	public final String getTitle() {
		return asTextWithDefault (jsonNode.path ("title"), null);
	}

	@Override
	public final String getAbstract() {
		return asTextWithDefault (jsonNode.path ("abstract"), null);
	}

	@Override
	public final Optional<Tiling> getTiling() {
		return
			getOptional(jsonNode, "tiling")
				.map(JsonTiling::new);
	}
	
	@Override
	public final boolean isConfidential () {
		return jsonNode.path ("confidential").asBoolean ();
	}
	
	@Override
	public final boolean isWmsOnly () {
		return jsonNode.path ("wmsOnly").asBoolean ();
	}
	
	@Override
	public final Optional<Timestamp> getImportTime() {
		return
			getOptional(jsonNode, "importTime")
				.map(JsonNode::asLong)
				.map(Timestamp::new);
	}
}
