package nl.idgis.publisher.service.json;

import static java.util.stream.Collectors.toList;
import static nl.idgis.publisher.service.json.JsonService.getStream;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;

public class JsonGroupLayer extends AbstractJsonLayer implements GroupLayer {
	
	private final Map<String, Optional<String>> metadataFileIdentifications;
	
	public JsonGroupLayer(JsonNode jsonNode, Map<String, Optional<String>> metadataFileIdentifications) {
		super(jsonNode);
		
		this.metadataFileIdentifications = metadataFileIdentifications;
	}	

	@Override
	public List<LayerRef<? extends Layer>> getLayers() {
		return 
			getStream(jsonNode, "layers")
				.map(jsonNode -> AbstractJsonLayerRef.fromJson(jsonNode, metadataFileIdentifications))
				.collect(toList());
	}
	
}
