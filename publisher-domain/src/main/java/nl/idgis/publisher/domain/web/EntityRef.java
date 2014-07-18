package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class EntityRef extends Entity {

	private static final long serialVersionUID = -7795092129566328698L;
	
	public final EntityType type;
	public final String id;
	public final String name;
	
	@JsonCreator
	public EntityRef (final @JsonProperty("type") EntityType type, final @JsonProperty("id") String id, final @JsonProperty("name") String name) {
		this.type = type;
		this.id = id;
		this.name = name;
	}
	
	@JsonGetter
	public EntityType type () {
		return type;
	}
	
	@JsonGetter
	public String id () {
		return id;
	}
	
	@JsonGetter
	public String name () {
		return name;
	}
}
