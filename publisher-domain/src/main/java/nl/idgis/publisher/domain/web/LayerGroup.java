/**
 *
 */
package nl.idgis.publisher.domain.web;

import java.util.List;
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
	
	private static final long serialVersionUID = -2055292766835854817L;
	
	private final String name;
	private final String title;
	private final String abstractText;
	private final List<String> userGroups;
	
	private final TiledLayer tiledLayer;
	
	private final boolean confidential;
	
	private final boolean wmsOnly;
	

	@JsonCreator
	@QueryProjection
	public LayerGroup(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String name, 
			final @JsonProperty("") String title, 
			final @JsonProperty("") String abstractText,
			final @JsonProperty("") List<String> userGroups,
			final @JsonProperty("") TiledLayer tiledLayer,
			final @JsonProperty("confidential") boolean confidential,
			final @JsonProperty("wmsOnly") boolean wmsOnly) {
		super(id);
		this.name = name;
		this.title = title;
		this.abstractText = abstractText;
		this.userGroups = userGroups;
		this.tiledLayer = tiledLayer;
		this.confidential = confidential;
		this.wmsOnly = wmsOnly;
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
	public List<String> userGroups() {
		return userGroups;
	}

	public Optional<TiledLayer> tiledLayer() {
		return Optional.ofNullable(tiledLayer);
	}

	@Override
	@JsonGetter
	public boolean confidential () {
		return this.confidential;
	}
	
	@Override
	@JsonGetter
	public boolean wmsOnly () {
		return this.wmsOnly;
	}
}