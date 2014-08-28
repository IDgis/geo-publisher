package nl.idgis.publisher.domain.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.web.EntityType;

public final class DatasetNotification extends MessageProperties {

	private static final long serialVersionUID = 4482059888625110486L;

	@JsonCreator
	public DatasetNotification (
			final @JsonProperty("entityType") EntityType entityType, 
			final @JsonProperty("identification") String identification,
			final @JsonProperty("title") String title) {
		super(entityType, identification, title);
	}
}
