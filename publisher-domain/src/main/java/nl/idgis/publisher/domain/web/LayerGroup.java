/**
 *
 */
package nl.idgis.publisher.domain.web;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

/**
 * Representation of a group.
 * 
 * @author Rob
 *
 */
public class LayerGroup extends Identifiable implements Selectable {
	private static final long serialVersionUID = 6882751274387765408L;

	private final String name;
	private final String title;
	private final String abstractText;
	private final Boolean published;
	
	private final TiledLayer tiledLayer;
	
	private final boolean confidential;
	

	@JsonCreator
	@QueryProjection
	public LayerGroup(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String name, 
			final @JsonProperty("") String title, 
			final @JsonProperty("") String abstractText,
			final @JsonProperty("") Boolean published,
			final @JsonProperty("") TiledLayer tiledLayer,
			final @JsonProperty("confidential") boolean confidential) {
		super(id);
		this.name = name;
		this.title = title;
		this.abstractText = abstractText;
		this.published = published;
		this.tiledLayer = tiledLayer;
		this.confidential = confidential;
	}

	@JsonGetter
	public String name() {
		return name;
	}

	@JsonGetter
	public String title() {
		return title;
	}

	@JsonGetter
	public String abstractText() {
		return abstractText;
	}
	
	@JsonGetter
	public Boolean published() {
		return published;
	}

	public Optional<TiledLayer> tiledLayer() {
		return Optional.ofNullable(tiledLayer);
	}

	@Override
	@JsonGetter
	public boolean confidential () {
		return this.confidential;
	}
}