package nl.idgis.publisher.domain.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.idgis.publisher.domain.EntityType;

public final class NotificationProperties extends JobMessageProperties {
	
	private static final long serialVersionUID = -6151619845009714881L;
	
	private final ConfirmNotificationResult result;
	private final String createTime;
	
	@JsonCreator
	public NotificationProperties (
			final @JsonProperty("entityType") EntityType entityType, 
			final @JsonProperty("identification") String identification,
			final @JsonProperty("title") String title,
			final @JsonProperty("createTime") String createTime,
			final @JsonProperty("result") ConfirmNotificationResult result) {
		super(entityType, identification, title);
		
		this.createTime = createTime;
		this.result = result;
	}
	
	public String getCreateTime () {
		return createTime;
	}
	
	public ConfirmNotificationResult getResult () {
		return result;
	}
}
