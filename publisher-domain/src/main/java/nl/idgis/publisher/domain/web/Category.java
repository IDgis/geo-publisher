package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Category extends Identifiable {
	
	private static final long serialVersionUID = 1478942821142620485L;
	
	private final String name;
	
	@JsonCreator
	public Category (final @JsonProperty("id") String id, final @JsonProperty("name") String name) {
		super (id);

		this.name = name;
	}

	@JsonGetter
	public String name () {
		return this.name;
	}
}
