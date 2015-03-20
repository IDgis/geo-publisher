package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public interface LayerContainer {

	List<LayerRef<? extends Layer>> getLayers();
}
