/**
 *
 */
package nl.idgis.publisher.domain.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

/**
 * Representation of a tiled layer.
 * 
 * @author Rob
 *
 */
public class TiledLayer extends Identifiable {
	private static final long serialVersionUID = 3748360159816460138L;

	private final Boolean enabled;
	private final Integer metaWidth;
	private final Integer metaHeight;
	private final Integer expireCache;
	private final Integer expireClients;
	private final Integer gutter;
	
	@JsonCreator
	@QueryProjection
	public TiledLayer(
			final @JsonProperty("") String id, 
			final @JsonProperty("") Integer metaWidth,
			final @JsonProperty("") Integer metaHeight,
			final @JsonProperty("") Integer expireCache,
			final @JsonProperty("") Integer expireClients,
			final @JsonProperty("") Integer gutter,
			final @JsonProperty("") Boolean enabled) {
		super(id);
		this.metaWidth = metaWidth;
		this.enabled = enabled;
		this.metaHeight = metaHeight;
		this.expireCache = expireCache;
		this.expireClients = expireClients;
		this.gutter = gutter;
	}

	@JsonGetter
	public Integer metaWidth() {
		return metaWidth;
	}

	@JsonGetter
	public Integer metaHeight() {
		return metaHeight;
	}

	@JsonGetter
	public Integer expireCache() {
		return expireCache;
	}

	@JsonGetter
	public Integer expireClients() {
		return expireClients;
	}

	@JsonGetter
	public Integer gutter() {
		return gutter;
	}

	@JsonGetter
	public Boolean enabled() {
		return enabled;
	}

}