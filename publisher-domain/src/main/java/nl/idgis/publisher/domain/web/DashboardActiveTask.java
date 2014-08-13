package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;


public final class DashboardActiveTask extends Identifiable {

	private static final long serialVersionUID = 5377556601944221922L;

	private final String datasetName;
	private final String message;
	private final Integer progress;
	
	@JsonCreator
	public DashboardActiveTask(
			final @JsonProperty("id") String id, 
			final @JsonProperty("datasetName") String datasetName, 
			final @JsonProperty("message") String message,
			final @JsonProperty("progress") Integer progress) {
		super(id);
		this.datasetName = datasetName;
		this.message = message;
		this.progress = progress;
	}
	
	@JsonGetter
	public String datasetName() {
		return datasetName;
	}
	@JsonGetter
	public String message() {
		return message;
	}
	@JsonGetter
	public Integer progress(){
		return progress;
	}

	@Override
	public String toString() {
		return "DashboardActiveTask [datasetName=" + datasetName + ", message="
				+ message + ", progress=" + progress + "]";
	}
	
}
