package nl.idgis.publisher.service.manager.messages;

import java.util.List;

public interface Service {

	String getId();
	String getRootId();
	List<Layer> getLayers();
}
