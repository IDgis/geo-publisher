package nl.idgis.publisher.harvester.messages;

import java.io.Serializable;

public class DataSourceConnected implements Serializable {	
	
	private static final long serialVersionUID = -6107698322151511761L;
	
	private final String dataSourceName;
	
	public DataSourceConnected(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	public String getDataSourceName() {
		return dataSourceName;
	}

	@Override
	public String toString() {
		return "DataSourceConnected [dataSourceName=" + dataSourceName + "]";
	}
}
