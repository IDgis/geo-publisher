package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.idgis.publisher.domain.job.JobMessageProperties;

public final class DefaultJobMessageProperties extends JobMessageProperties {

	private static final long serialVersionUID = 441632662855383476L;

	@JsonCreator
	public DefaultJobMessageProperties(@JsonProperty("entityType") EntityType entityType,
			@JsonProperty("identification") String identification, @JsonProperty("title") String title) {
		super(entityType, identification, title);
	}

}
