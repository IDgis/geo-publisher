package nl.idgis.publisher.harvester.messages;

import java.io.Serializable;

public class Harvest implements Serializable {
		
	private static final long serialVersionUID = 1557912124784652506L;
	
	private final String dataSourceName;
	
	public Harvest() {
		this(null);
	}
	
	public Harvest(String name) {
		this.dataSourceName = name;
	}
	
	public String getDataSourceName() {
		return dataSourceName;
	}

	@Override
	public String toString() {
		return "Harvest [getSourceName=" + dataSourceName + "]";
	}
	
}
