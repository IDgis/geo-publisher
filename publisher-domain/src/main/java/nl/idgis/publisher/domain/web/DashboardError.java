package nl.idgis.publisher.domain.web;

import org.joda.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;


public final class DashboardError extends Identifiable {

	private static final long serialVersionUID = 5377556601944221922L;

	private final String datasetName;
	private final String message;
	private final LocalDateTime dateTime;
	
	@JsonCreator
	public DashboardError(
			final @JsonProperty("id") String id, 
			final @JsonProperty("datasetName") String datasetName, 
			final @JsonProperty("message") String message,
			final @JsonProperty("dateTime") LocalDateTime dateTime) {
		super(id);
		this.datasetName = datasetName;
		this.message = message;
		this.dateTime = dateTime;
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
	public LocalDateTime dateTime() {
		return dateTime;
	}

	@Override
	public String toString() {
		return "DashboardError [datasetName=" + datasetName + ", message="
				+ message + ", dateTime=" + dateTime + "]";
	}
	
}
