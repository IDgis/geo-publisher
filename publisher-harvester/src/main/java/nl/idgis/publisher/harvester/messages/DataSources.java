package nl.idgis.publisher.harvester.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

public class DataSources implements Serializable {
	
	private static final long serialVersionUID = -3449433061319167448L;
	
	private final Set<String> dataSourceId;
	
	public DataSources(Set<String> dataSourceId) {
		this.dataSourceId = dataSourceId;
	}
	
	public Set<String> getDataSourceNames() {
		return Collections.unmodifiableSet(dataSourceId);
	}

	@Override
	public String toString() {
		return "DataSourceList [dataSourceId=" + dataSourceId + "]";
	}
}
