package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public interface GroupLayer extends Layer, Group {

	List<LayerRef> getLayers();
}
