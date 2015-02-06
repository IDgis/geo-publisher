/**
 *
 */
package nl.idgis.publisher.domain.web;

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
public class LayerGroup extends Identifiable {
	private static final long serialVersionUID = 6882751274387765408L;

	private final String name;
	private final String title;
	private final String abstractText;
	private final Boolean published;
	
	@JsonCreator
	@QueryProjection
	public LayerGroup(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String name, 
			final @JsonProperty("") String title, 
			final @JsonProperty("") String abstractText,
			final @JsonProperty("") Boolean published) {
		super(id);
		this.name = name;
		this.title = title;
		this.abstractText = abstractText;
		this.published = published;
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
	

}