package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public interface GroupLayer extends Layer {

	List<LayerRef<? extends Layer>> getLayers();
}
