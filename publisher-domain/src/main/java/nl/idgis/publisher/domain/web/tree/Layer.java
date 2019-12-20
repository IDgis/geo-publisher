package nl.idgis.publisher.domain.web.tree;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface Layer  {
	
	String getId();
	
	String getName();
	
	String getTitle();
	
	String getAbstract();
	
	List<String> getUserGroups();
	
	Optional<Tiling> getTiling();
	
	boolean isConfidential ();
	
	Optional<Timestamp> getImportTime();

	boolean isWmsOnly();
}
