package nl.idgis.publisher.database.messages;

import com.mysema.query.annotations.QueryProjection;

public class HarvestJobInfo extends JobInfo {

	private static final long serialVersionUID = -723392023585391066L;
	
	private final String dataSourceId;
	
	@QueryProjection
	public HarvestJobInfo(int id, String dataSourceId) {
		super(id);
		
		this.dataSourceId = dataSourceId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	@Override
	public String toString() {
		return "HarvestJob [dataSourceId=" + dataSourceId + "]";
	}
}
