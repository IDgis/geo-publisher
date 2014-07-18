package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Job extends Identifiable {

	private static final long serialVersionUID = 1200400080491656842L;
	
	private final Status initialStatus;
	private final Status currentStatus;
	
	@JsonCreator
	public Job (
			final @JsonProperty("id") String id,
			final @JsonProperty("initialStatus") Status initialStatus,
			final @JsonProperty("status") Status currentStatus) {
		
		super (id);
		
		this.initialStatus = initialStatus;
		this.currentStatus = currentStatus;
	}
	
	@JsonGetter
	public Status initialStatus () {
		return initialStatus;
	}
	
	public Status currentStatus () {
		return currentStatus;
	}
}
