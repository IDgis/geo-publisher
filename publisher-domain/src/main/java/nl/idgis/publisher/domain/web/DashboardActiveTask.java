package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.JobStateType;
import nl.idgis.publisher.domain.JobType;
import nl.idgis.publisher.domain.NotificationType;

import org.joda.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;


public final class DashboardActiveTask extends Notification {

	private static final long serialVersionUID = 5377556601944221922L;

	@JsonCreator
	public DashboardActiveTask(final @JsonProperty("id") String id, 
			final @JsonProperty("type") NotificationType type, 
			final @JsonProperty("message") Message message,
			final @JsonProperty("datasetName") String datasetName,
			final @JsonProperty("payload") Object payload, 
			final @JsonProperty("jobState") JobStateType jobState, 
			final @JsonProperty("jobType") JobType jobType,
			final @JsonProperty("dateTime") LocalDateTime dateTime) {
		super(id, type, message, datasetName, payload, jobState, jobType, dateTime);
	}


}
