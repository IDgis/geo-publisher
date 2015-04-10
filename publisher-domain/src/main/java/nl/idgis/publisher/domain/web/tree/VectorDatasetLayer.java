package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public interface VectorDatasetLayer extends DatasetLayer {

	String getTableName();
	
	List<String> getColumnNames();
}
