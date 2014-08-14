package nl.idgis.publisher.database.messages;

import com.mysema.query.annotations.QueryProjection;

public class HarvestJob extends Job {

	private static final long serialVersionUID = -723392023585391066L;
	
	private final String dataSourceId;
	
	@QueryProjection
	public HarvestJob(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}
}
