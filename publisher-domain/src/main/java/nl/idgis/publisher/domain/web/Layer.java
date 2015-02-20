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
 * Representation of a layer.
 * 
 * @author Rob
 *
 */
public class Layer extends Identifiable {
	private static final long serialVersionUID = -6332996024596175388L;

	private final String name;
	private final String title;
	private final String abstractText;
	private final List<String> keywords;
	private final Boolean published;
	private final String datasetId;

	
	@JsonCreator
	@QueryProjection
	public Layer(
			final @JsonProperty("") String id,
			final @JsonProperty("") String name, 
			final @JsonProperty("") String title, 
			final @JsonProperty("") String abstractText,
			final @JsonProperty("") List<String> keywords,
			final @JsonProperty("") Boolean published,
			final @JsonProperty("") String datasetId 
			) {
		super(id);
		this.name = name;
		this.title = title;
		this.abstractText = abstractText;
		this.keywords = keywords;
		this.published = published;
		this.datasetId = datasetId;
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
	public List<String> keywords() {
		return keywords;
	}

	@JsonGetter
	public Boolean published() {
		return published;
	}

	public String datasetId() {
		return datasetId;
	}

}