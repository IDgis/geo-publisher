package nl.idgis.publisher.domain.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class DatabaseLog extends DatasetLog<DatabaseLog> {

	private static final long serialVersionUID = 8126046350738527476L;
	
	private final String tableName;

	@JsonCreator
	public DatabaseLog(
			@JsonProperty("tableName") String tableName) {
		
		this.tableName = tableName;
	}
	
	private DatabaseLog(Dataset dataset, String tableName) {
		super(dataset);
		
		this.tableName = tableName;
	}
	
	public String getTableName() {
		return this.tableName;
	}
	
	@Override
	public DatabaseLog withDataset(Dataset dataset) {
		return new DatabaseLog(dataset, tableName);
	}

	@Override
	public String toString() {
		return "DatabaseLog [tableName=" + tableName + "]";
	}
}
