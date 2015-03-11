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
	private final String oldName;
	private final String definition;
	private final StyleType styleType;
	private final Boolean inUse;
	
	public static final String NAME_EXISTS = "#NAME_EXISTS#";

	@JsonCreator
	@QueryProjection
	public Style(
			final @JsonProperty("id") String id, 
			final @JsonProperty("name") String name,
			final @JsonProperty("oldName") String oldName,
			final @JsonProperty("definition") String definition,
			final @JsonProperty("styleType") String styleType,
			final @JsonProperty("inUse") Boolean inUse) {
		super(id);
		this.name = name;
		this.oldName = oldName;
		this.definition = definition;
		this.styleType = StyleType.valueOf(styleType);
		this.inUse = inUse;
	}

	@JsonGetter
	public String name() {
		return name;
	}

	public String oldName() {
		return oldName;
	}

	@JsonGetter
	public String definition() {
		return definition;
	}
	
	@JsonGetter
	public StyleType styleType() {
		return styleType;
	}
	
	@JsonGetter
	public Boolean inUse() {
		return inUse;
	}
}