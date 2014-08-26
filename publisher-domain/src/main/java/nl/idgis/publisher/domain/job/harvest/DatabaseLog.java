package nl.idgis.publisher.domain.job.harvest;

import nl.idgis.publisher.domain.web.EntityType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatabaseLog extends HarvestLog {

	private static final long serialVersionUID = -8247769014457037946L;
	
	private final String tableName;

	@JsonCreator
	public DatabaseLog(
			@JsonProperty("entityType") EntityType entityType,
			@JsonProperty("identification") String identification,
			@JsonProperty("title") String title,
			@JsonProperty("alternateTitle") String alternateTitle,
			@JsonProperty("tableName") String tableName) {
		
		super(entityType, identification, title, alternateTitle);
		
		this.tableName = tableName;
	}
	
	public String getTableName() {
		return this.tableName;
	}

	@Override
	public String toString() {
		return "DatabaseTableName [tableName=" + tableName
				+ ", identification=" + getIdentification () + "]";
	}

}
