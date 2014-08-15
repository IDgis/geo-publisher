package nl.idgis.publisher.domain.job.harvest;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SourceDatasetRegistration implements Serializable {

	private static final long serialVersionUID = 4481594525226023546L;

	private final String sourceDatasetId;
	
	@JsonCreator
	public SourceDatasetRegistration(
			@JsonProperty("sourceDatasetId") String sourceDatasetId) {
		
		this.sourceDatasetId = sourceDatasetId;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}	
}
