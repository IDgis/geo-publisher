/**
 *
 */
package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

/**
 * Representation of user defined style.
 * 
 * @author Rob
 *
 */
public class Style extends Identifiable {
	private static final long serialVersionUID = -103047524556298813L;
	private final String name;
	private final String definition;
	private final StyleType styleType;

	@JsonCreator
	@QueryProjection
	public Style(
			final @JsonProperty("id") String id, 
			final @JsonProperty("name") String name,
			final @JsonProperty("definition") String definition,
			final @JsonProperty("styleType") String styleType) {
		super(id);
		this.name = name;
		this.definition = definition;
		this.styleType = StyleType.valueOf(styleType);
	}

	@JsonGetter
	public String name() {
		return name;
	}

	@JsonGetter
	public String definition() {
		return definition;
	}
	
	@JsonGetter
	public StyleType styleType() {
		return styleType;
	}
}