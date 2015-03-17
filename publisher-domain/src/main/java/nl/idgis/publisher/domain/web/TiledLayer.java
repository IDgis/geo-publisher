/**
 *
 */
package nl.idgis.publisher.domain.web;

import java.io.Serializable;
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

	private static final long serialVersionUID = -2829845147879657871L;
	
	private final String name;
	private final Integer metaWidth;
	private final Integer metaHeight;
	private final Integer expireCache;
	private final Integer expireClients;
	private final Integer gutter;
	
	private final List<String> mimeformats;
	
	@JsonCreator
	@QueryProjection
	public TiledLayer(
			final @JsonProperty("") String id,
			final @JsonProperty("") String name, 
			final @JsonProperty("") Integer metaWidth,
			final @JsonProperty("") Integer metaHeight,
			final @JsonProperty("") Integer expireCache,
			final @JsonProperty("") Integer expireClients,
			final @JsonProperty("") Integer gutter,
			 List<String> mimeformats) {
		super(id);
		this.name = name;
		this.metaWidth = metaWidth;
		this.metaHeight = metaHeight;
		this.expireCache = expireCache;
		this.expireClients = expireClients;
		this.gutter = gutter;
		this.mimeformats = mimeformats;
	}	

	@JsonGetter
	public String name() {
		return name;
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
	
	public List<String> mimeformats() {
		return mimeformats;
	}

}