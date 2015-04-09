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

	private static final long serialVersionUID = 8939482151862501678L;
	
	private final String name;
	private final String title;
	private final String abstractText;
	private final Boolean published;
	private final String datasetId;
	private final String datasetName;
	
	private final TiledLayer tiledLayer;
	
	private final List<String> keywords;
	private final List<Style> styles;
	
	private final boolean confidential;

	
	@JsonCreator
	@QueryProjection
	public Layer(
			final @JsonProperty("") String id,
			final @JsonProperty("") String name, 
			final @JsonProperty("") String title, 
			final @JsonProperty("") String abstractText,
			final @JsonProperty("") Boolean published,
			final @JsonProperty("") String datasetId,
			final @JsonProperty("") String datasetName,
			final @JsonProperty("") TiledLayer tiledLayer,
			final @JsonProperty("") List<String> keywords,
			final @JsonProperty("") List<Style> styles,
			final @JsonProperty("confidential") boolean confidential) {
		super(id);
		this.name = name;
		this.title = title;
		this.abstractText = abstractText;
		this.published = published;
		this.datasetId = datasetId;
		this.datasetName = datasetName;
		this.tiledLayer = tiledLayer;
		this.keywords = keywords;
		this.styles = styles;
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
	
	public List<String> getKeywords() {
		return keywords;
	}

	public List<Style> styles() {
		return styles;
	}

	@JsonGetter
	public boolean confidential () {
		return this.confidential;
	}
}