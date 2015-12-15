package nl.idgis.publisher.service.json;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.RasterDatasetLayer;

public class JsonRasterDatasetLayer extends AbstractJsonDatasetLayer implements RasterDatasetLayer {

	public JsonRasterDatasetLayer(JsonNode jsonNode, Optional<String> metadataFileIdentification) {
		super(jsonNode, metadataFileIdentification);
	}
	
	@Override
	public boolean isRasterLayer() {
		return true;
	}

	@Override
	public RasterDatasetLayer asRasterLayer() {
		return this;
	}

	@Override
	public String getFileName() {		
		return jsonNode.get("fileName").asText();
	}
}
