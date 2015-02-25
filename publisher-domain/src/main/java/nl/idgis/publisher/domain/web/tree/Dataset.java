package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public interface Dataset extends Item {
	
	List<String> getKeywords();
	
	String getTableName();
	
	List<String> getStyles();
}
