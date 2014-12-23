package nl.idgis.publisher.domain.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatabaseLog extends DatasetLog {

	private static final long serialVersionUID = 8126046350738527476L;
	
	private final String tableName;

	@JsonCreator
	public DatabaseLog(
			@JsonProperty("tableName") String tableName) {
		
		this.tableName = tableName;
	}
	
	public String getTableName() {
		return this.tableName;
	}

	@Override
	public String toString() {
		return "DatabaseLog [tableName=" + tableName + "]";
	}	

}
