package nl.idgis.publisher.service.geoserver.rest;

import java.util.Objects;
import java.util.Optional;

public class GridSubset {
	
	private final String gridSetName;
	
	private final Integer minCachedLevel;
	
	private final Integer maxCachedLevel;
	
	public GridSubset(String gridSetName) {
		this(gridSetName, Optional.empty(), Optional.empty());
	}
	
	public GridSubset(String gridSetName, Optional<Integer> minCachedLevel, Optional<Integer> maxCachedLevel) {
		this.gridSetName = Objects.requireNonNull(gridSetName);
		this.minCachedLevel = minCachedLevel.orElse(null);
		this.maxCachedLevel = maxCachedLevel.orElse(null);
	}
	
	public String getGridSetName() {
		return gridSetName;
	}
	
	public Optional<Integer> getMinCachedLevel() {
		return Optional.ofNullable(minCachedLevel);
	}
	
	public Optional<Integer> getMaxCachedLevel() {
		return Optional.ofNullable(maxCachedLevel);
	}

	@Override
	public String toString() {
		return "GridSubset [gridSetName=" + gridSetName + ", minCachedLevel="
				+ minCachedLevel + ", maxCachedLevel=" + maxCachedLevel + "]";
	}

}
