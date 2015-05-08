package nl.idgis.publisher.service.geoserver.rest;

import java.util.Objects;

public class GridSubset {
	
	private final String gridSetName;
	
	public GridSubset(String gridSetName) {
		this.gridSetName = Objects.requireNonNull(gridSetName);
	}
	
	public String getGridSetName() {
		return gridSetName;
	}
	
	@Override
	public String toString() {
		return "GridSubset [gridSetName=" + gridSetName + "]";
	}

}
