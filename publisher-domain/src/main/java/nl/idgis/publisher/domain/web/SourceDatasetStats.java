package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SourceDatasetStats extends Entity {

	private static final long serialVersionUID = 3620755264710033479L;
	
	private final SourceDataset 	sourceDataset;
	private final long				datasetCount;
	
	@JsonCreator
	public SourceDatasetStats (
			final @JsonProperty("sourceDataset") SourceDataset sourceDataset, 
			final @JsonProperty("datasetCount") long datasetCount) {
		
		
		this.sourceDataset = sourceDataset;
		this.datasetCount = datasetCount;
	}

	@JsonGetter
	public SourceDataset sourceDataset () {
		return this.sourceDataset;
	}
	
	@JsonGetter
	public long datasetCount () {
		return this.datasetCount;
	}
}
