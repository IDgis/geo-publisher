package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.JobStateType;
import nl.idgis.publisher.domain.JobType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Issue extends DashboardItem {

	private static final long serialVersionUID = 5377556601944221922L;
	
	private final JobStateType jobState;
	private final JobType jobType;
	
	@JsonCreator
	public Issue(
			final @JsonProperty("id") String id, 
			final @JsonProperty("message") Message message,			 
			final @JsonProperty("jobState") JobStateType jobState, 
			final @JsonProperty("jobType") JobType jobType) {
		
		super(id, message);
		
		this.jobState = jobState;
		this.jobType = jobType;
	}

	@JsonGetter
	public JobStateType jobState() {
		return jobState;
	}

	@JsonGetter
	public JobType jobType() {
		return jobType;
	}

}
