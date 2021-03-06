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
 * Representation of a layer.
 * 
 * @author Rob
 *
 */
public class Layer extends Identifiable implements Selectable {
	
	private static final long serialVersionUID = -6064637318682660544L;
	
	private final String name;
	private final String title;
	private final String abstractText;
	private final String datasetId;
	private final String datasetName;
	
	private final TiledLayer tiledLayer;
	
	private final List<String> keywords;
	private final List<Style> styles;
	private final List<String> userGroups;
	
	private final boolean confidential;
	
	private final boolean wmsOnly;

	
	@JsonCreator
	@QueryProjection
	public Layer(
			final @JsonProperty("") String id,
			final @JsonProperty("") String name, 
			final @JsonProperty("") String title, 
			final @JsonProperty("") String abstractText,
			final @JsonProperty("") String datasetId,
			final @JsonProperty("") String datasetName,
			final @JsonProperty("") TiledLayer tiledLayer,
			final @JsonProperty("") List<String> keywords,
			final @JsonProperty("") List<Style> styles,
			final @JsonProperty("") List<String> userGroups,
			final @JsonProperty("confidential") boolean confidential,
			final @JsonProperty("wmsOnly") boolean wmsOnly) {
		super(id);
		this.name = name;
		this.title = title;
		this.abstractText = abstractText;
		this.datasetId = datasetId;
		this.datasetName = datasetName;
		this.tiledLayer = tiledLayer;
		this.keywords = keywords;
		this.styles = styles;
		this.userGroups = userGroups;
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

	public String datasetId() {
		return datasetId;
	}
	
	@JsonGetter
	public String datasetName() {
		return datasetName;
	}
	
	public Optional<TiledLayer> tiledLayer() {
		return Optional.ofNullable(tiledLayer);
	}
	
	public List<String> keywords() {
		return keywords;
	}

	public List<Style> styles() {
		return styles;
	}
	
	public List<String> userGroups() {
		return userGroups;
	}

	@JsonGetter
	public boolean confidential () {
		return this.confidential;
	}

	@JsonGetter
	public boolean wmsOnly () {
		return this.wmsOnly;
	}
}