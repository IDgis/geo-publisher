package nl.idgis.publisher.domain.web.tree;

import java.util.Optional;

public interface Item {

	String getId();
	
	String getName();
	
	String getTitle();
	
	String getAbstract();
	
	Optional<TilingSettings> getTilingSettings();
}
