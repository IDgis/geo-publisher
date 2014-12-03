package nl.idgis.publisher.domain.job.load;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.JobMessageProperties;
import nl.idgis.publisher.domain.web.EntityType;

public final class ImportNotificationProperties extends JobMessageProperties {

	private static final long serialVersionUID = 4482059888625110486L;

	private final ConfirmNotificationResult result;
	
	@JsonCreator
	public ImportNotificationProperties (
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
