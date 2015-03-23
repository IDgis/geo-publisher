package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public interface DatasetLayer extends Layer {
	
	List<String> getKeywords();
	
	String getTableName();
	
	List<StyleRef> getStyleRefs();
}
