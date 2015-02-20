package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class TiledLayer {
	
	private final String name;
	
	private final List<String> mimeFormats;
	
	private final Integer metaWidth, metaHeight, expireCache, expireClients, gutter;
	
	public TiledLayer(String name, List<String> mimeFormats, Integer metaWidth, Integer metaHeight, Integer expireCache, Integer expireClients, Integer gutter) {
		this.name = name;
		this.mimeFormats = mimeFormats;
		this.metaWidth = metaWidth;
		this.metaHeight = metaHeight;
		this.expireCache = expireCache;
		this.expireClients = expireClients;
		this.gutter = gutter;
	}

	public String getName() {
		return name;
	}
	
	public List<String> getMimeFormats() {
		return mimeFormats;
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
		return "TiledLayer [name=" + name + ", mimeFormats=" + mimeFormats
				+ ", metaWidth=" + metaWidth + ", metaHeight=" + metaHeight
				+ ", expireCache=" + expireCache + ", expireClients="
				+ expireClients + ", gutter=" + gutter + "]";
	}
	
}
