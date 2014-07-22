package nl.idgis.publisher.harvester.messages;

import java.io.Serializable;

public class DataSourceConnected implements Serializable {	
	
	private static final long serialVersionUID = -6107698322151511761L;
	
	private final String dataSourceId;
	
	public DataSourceConnected(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	@Override
	public String toString() {
		return "DataSourceConnected [dataSourceId=" + dataSourceId + "]";
	}
}
