package nl.idgis.publisher.domain.web.tree;

import java.util.Optional;

public interface Layer  {
	
	String getId();
	
	String getName();
	
	String getTitle();
	
	String getAbstract();
	
	Optional<Tiling> getTiling();
	
	boolean isConfidential ();
	
}
