package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class TiledLayer {
	
	private final List<String> mimeFormats;
	
	private final List<GridSubset> gridSubsets;
	
	private final Integer metaWidth, metaHeight, expireCache, expireClients, gutter;
	
	public TiledLayer(List<String> mimeFormats, List<GridSubset> gridSubsets, Integer metaWidth, Integer metaHeight, Integer expireCache, Integer expireClients, Integer gutter) {		
		this.mimeFormats = mimeFormats;
		this.gridSubsets = gridSubsets;
		this.metaWidth = metaWidth;
		this.metaHeight = metaHeight;
		this.expireCache = expireCache;
		this.expireClients = expireClients;
		this.gutter = gutter;
	}	
	
	public List<String> getMimeFormats() {
		return mimeFormats;
	}
	
	public List<GridSubset> getGridSubsets() {
		return gridSubsets;
	}

	public Integer getMetaWidth() {
		return metaWidth;
	}

	public Integer getMetaHeight() {
		return metaHeight;
	}

	public Integer getExpireCache() {
		return expireCache;
	}

	public Integer getExpireClients() {
		return expireClients;
	}

	public Integer getGutter() {
		return gutter;
	}

	@Override
	public String toString() {
		return "TiledLayer [mimeFormats=" + mimeFormats + ", gridSubsets="
				+ gridSubsets + ", metaWidth=" + metaWidth + ", metaHeight="
				+ metaHeight + ", expireCache=" + expireCache
				+ ", expireClients=" + expireClients + ", gutter=" + gutter
				+ "]";
	}
	
}
