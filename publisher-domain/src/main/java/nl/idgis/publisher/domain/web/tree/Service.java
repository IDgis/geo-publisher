package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public interface Service {

	String getId();
	
	String getRootId();
	
	List<Layer> getLayers();
}
