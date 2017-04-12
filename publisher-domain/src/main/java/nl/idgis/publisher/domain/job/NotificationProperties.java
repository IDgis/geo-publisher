package nl.idgis.publisher.domain.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.idgis.publisher.domain.EntityType;

public final class NotificationProperties extends JobMessageProperties {

	private static final long serialVersionUID = 4482059888625110486L;

	private final ConfirmNotificationResult result;
	
	@JsonCreator
	public NotificationProperties (
			final @JsonProperty("entityType") EntityType entityType, 
			final @JsonProperty("identification") String identification,
			final @JsonProperty("title") String title,
			final @JsonProperty("result") ConfirmNotificationResult result) {
		super(entityType, identification, title);
		
		this.result = result;
	}

	public ConfirmNotificationResult getResult () {
		return result;
	}
}
