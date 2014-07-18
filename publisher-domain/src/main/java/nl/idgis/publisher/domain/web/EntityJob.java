package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityJob extends Entity {

	private static final long serialVersionUID = 5430126981727462961L;
	
	private final EntityRef entity;
	private final Job job;
	
	@JsonCreator
	public EntityJob (final @JsonProperty EntityRef entity, final @JsonProperty Job job) {
		this.entity = entity;
		this.job = job;
	}
	
	@JsonGetter
	public EntityRef entity () {
		return entity;
	}
	
	@JsonGetter
	public Job job () { 
		return job;
	}
}
