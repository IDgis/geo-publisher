package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.List;

public class DefaultTiling implements Tiling, Serializable {
	
	private static final long serialVersionUID = 2925287832249903424L;

	private final List<String> mimeFormats;
	
	private final Integer metaWidth, metaHeight, expireCache, expireClients, gutter;

	public DefaultTiling(List<String> mimeFormats, Integer metaWidth, Integer metaHeight, Integer expireCache, 
		Integer expireClients, Integer gutter) {
		
		this.mimeFormats = mimeFormats;
		this.metaWidth = metaWidth;
		this.metaHeight = metaHeight;
		this.expireCache = expireCache;
		this.expireClients = expireClients;
		this.gutter = gutter;
	}
	
	@Override
	public List<String> getMimeFormats() {
		return mimeFormats;
	}

	@Override
	public Integer getMetaWidth() {
		return metaWidth;
	}

	@Override
	public Integer getMetaHeight() {
		return metaHeight;
	}

	@Override
	public Integer getExpireCache() {
		return expireCache;
	}

	@Override
	public Integer getExpireClients() {
		return expireClients;
	}

	@Override
	public Integer getGutter() {
		return gutter;
	}

}
