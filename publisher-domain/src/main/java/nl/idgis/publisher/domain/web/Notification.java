package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.JobStateType;
import nl.idgis.publisher.domain.JobType;
import nl.idgis.publisher.domain.NotificationType;

import org.joda.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Notification extends Identifiable {

	private static final long serialVersionUID = -6668318918440675297L;

	private final NotificationType type;
	private final Message message;
	private final String datasetName;
	private final Object payload;
	private final JobStateType jobState;
	private final JobType jobType;
	private final LocalDateTime dateTime;
	
	@JsonCreator
	public Notification(
			final @JsonProperty("id") String id, 
			final @JsonProperty("type") NotificationType type, 
			final @JsonProperty("message") Message message,
			final @JsonProperty("datasetName") String datasetName,
			final @JsonProperty("payload") Object payload, 
			final @JsonProperty("jobState") JobStateType jobState, 
			final @JsonProperty("jobType") JobType jobType,
			final @JsonProperty("dateTime") LocalDateTime dateTime) {
		super(id);
		this.type = type;
		this.message = message;
		this.datasetName = datasetName;
		this.payload = payload;
		this.jobState = jobState;
		this.jobType = jobType;
		this.dateTime = dateTime;
	}
	
	@JsonGetter
	public NotificationType type () {
		return this.type;
	}
	
	@JsonGetter
	public Message message () {
		return this.message;
	}
	
	@JsonGetter
	public String datasetName () {
		return this.datasetName;
	}
	
	@JsonGetter
	public Object payload() {
		return payload;
	}

	@JsonGetter
	public JobStateType jobState() {
		return jobState;
	}

	@JsonGetter
	public JobType jobType() {
		return jobType;
	}

	@JsonGetter
	public LocalDateTime dateTime () {
		return this.dateTime;
	}

}
