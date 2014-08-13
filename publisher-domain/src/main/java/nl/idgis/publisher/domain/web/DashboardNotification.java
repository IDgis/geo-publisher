package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;


public final class DashboardNotification extends Identifiable {

	private static final long serialVersionUID = 5377556601944221922L;

	private final String datasetName;
	private final String message;
	
	@JsonCreator
	public DashboardNotification( 
			final @JsonProperty("id") String id,  
			final @JsonProperty("datasetName") String datasetName,  
			final @JsonProperty("message") String message) {
		super(id);
		this.datasetName = datasetName;
		this.message = message;
	}
	
	@JsonGetter
	public String datasetName() {
		return datasetName;
	}
	@JsonGetter
	public String message() {
		return message;
	}

	@Override
	public String toString() {
		return "DashboardNotification [datasetName=" + datasetName
				+ ", message=" + message + "]";
	}
}
