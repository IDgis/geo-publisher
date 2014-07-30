package nl.idgis.publisher.database.messages;

import java.io.Serializable;

import com.mysema.query.annotations.QueryProjection;

public class HarvestJob implements Serializable {

	private static final long serialVersionUID = -7943073067715468133L;
	
	private final String dataSourceId;
	
	@QueryProjection
	public HarvestJob(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}
}
