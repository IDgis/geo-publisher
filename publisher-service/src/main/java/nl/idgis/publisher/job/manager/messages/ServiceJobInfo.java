package nl.idgis.publisher.job.manager.messages;

import com.mysema.query.annotations.QueryProjection;

import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.domain.job.JobType;

public class ServiceJobInfo extends JobInfo {

	private static final long serialVersionUID = -2378569116461842009L;
	
	private final String schemaName, tableName;

	@QueryProjection
	public ServiceJobInfo(int id, String schemaName, String tableName) {
		super(id, JobType.SERVICE);
		
		this.schemaName = schemaName;
		this.tableName = tableName;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "ServiceJobInfo [schemaName=" + schemaName + ", tableName="
				+ tableName + ", id=" + id + "]";
	}
	
}
