package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.SourceDatasetType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SourceDatasetStats extends Entity {

	private static final long serialVersionUID = 3620755264710033479L;
	
	private final SourceDataset 	sourceDataset;
	private final long				datasetCount;
	private final SourceDatasetType	type;
	
	@JsonCreator
	public SourceDatasetStats (
			final @JsonProperty("sourceDataset") SourceDataset sourceDataset, 
			final @JsonProperty("datasetCount") long datasetCount,
			final @JsonProperty("type") SourceDatasetType type) {
		
		if (type == null) {
			throw new NullPointerException ("type cannot be null");
		}
		
		this.sourceDataset = sourceDataset;
		this.datasetCount = datasetCount;
		this.type = type;
	}

	@JsonGetter
	public SourceDataset sourceDataset () {
		return this.sourceDataset;
	}
	
	@JsonGetter
	public long datasetCount () {
		return this.datasetCount;
	}
	
	@JsonGetter
	public SourceDatasetType type () {
		return this.type;
	}
}
