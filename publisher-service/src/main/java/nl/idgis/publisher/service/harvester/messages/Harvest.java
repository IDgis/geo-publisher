package nl.idgis.publisher.service.harvester.messages;

import java.io.Serializable;

public class Harvest implements Serializable {
		
	private static final long serialVersionUID = 1557912124784652506L;
	
	private final String dataSourceId;
	
	public Harvest() {
		this(null);
	}
	
	public Harvest(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}
	
	public String getDataSourceId() {
		return dataSourceId;
	}

	@Override
	public String toString() {
		return "Harvest [dataSourceId=" + dataSourceId + "]";
	}
	
}
