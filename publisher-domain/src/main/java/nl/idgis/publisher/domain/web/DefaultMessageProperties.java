package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.idgis.publisher.domain.MessageProperties;

public final class DefaultMessageProperties extends MessageProperties {

	private static final long serialVersionUID = 441632662855383476L;

	@JsonCreator
	public DefaultMessageProperties(@JsonProperty("entityType") EntityType entityType,
			@JsonProperty("identification") String identification, @JsonProperty("title") String title) {
		super(entityType, identification, title);
	}

}
