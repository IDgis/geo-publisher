package nl.idgis.publisher.domain.web.tree;

import java.util.Optional;

public interface DatasetLayerRef extends LayerRef<DatasetLayer> {
	
	Optional<StyleRef> getStyleRef();
}
