package nl.idgis.publisher.service.manager.messages;

import com.mysema.query.annotations.QueryProjection;

public class DatasetNode extends Node implements Dataset {

	private static final long serialVersionUID = 8501260170166422665L;
	
	private final String tableName;
	
	@QueryProjection
	public DatasetNode(String id, String name, String title, String abstr, String tableName) {
		super(id, name, title, abstr);
		
		this.tableName = tableName;
	}	

	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "Dataset [tableName=" + tableName
				+ ", id=" + id + ", name=" + name + ", title=" + title
				+ ", abstr=" + abstr + "]";
	}	
}
