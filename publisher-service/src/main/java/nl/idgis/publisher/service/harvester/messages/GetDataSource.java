package nl.idgis.publisher.service.harvester.messages;

import java.io.Serializable;

public class GetDataSource implements Serializable {
	
	private static final long serialVersionUID = 2023564581868275036L;
	
	private final String dataSourceId;
	
	public GetDataSource(String dataSourceId) {
		this.dataSourceId = dataSourceId;
	}
	
	public String getDataSourceId() {
		return dataSourceId;
	}

	@Override
	public String toString() {
		return "GetDataSource [dataSourceId=" + dataSourceId + "]";
	}
}
