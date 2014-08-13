package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.JobStateType;
import nl.idgis.publisher.domain.JobType;
import nl.idgis.publisher.domain.NotificationType;

import org.joda.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;


public final class DashboardNotification extends Notification {

	private static final long serialVersionUID = -8244495374549919962L;

	@JsonCreator
	public DashboardNotification(
			final @JsonProperty("id") String id, 
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
