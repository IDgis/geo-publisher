package nl.idgis.publisher.service.json;

import static java.util.stream.Collectors.toList;
import static nl.idgis.publisher.service.json.JsonService.getStream;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.VectorDatasetLayer;

public class JsonVectorDatasetLayer extends AbstractJsonDatasetLayer implements VectorDatasetLayer {

	public JsonVectorDatasetLayer(JsonNode jsonNode, Optional<String> metadataFileIdentification) {
		super(jsonNode, metadataFileIdentification);
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
	public boolean isVectorLayer() {
		return true;
	}
	
	@Override
	public VectorDatasetLayer asVectorLayer() {
		return this;
	}
}
