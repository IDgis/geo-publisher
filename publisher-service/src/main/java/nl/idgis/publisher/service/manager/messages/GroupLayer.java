package nl.idgis.publisher.service.manager.messages;

import java.util.List;

public interface GroupLayer extends Layer, Group {

	List<Layer> getLayers();
}
