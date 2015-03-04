package nl.idgis.publisher.domain.web;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SourceDatasetStats extends Entity {

	private static final long serialVersionUID = 3620755264710033479L;
	
	private final SourceDataset 	sourceDataset;
	private final long				datasetCount;
	
	private final Message			lastLogMessage;
	private final Timestamp			lastLogTime;
	
	@JsonCreator
	public SourceDatasetStats (
			final @JsonProperty("sourceDataset") SourceDataset sourceDataset, 
			final @JsonProperty("datasetCount") long datasetCount,
			final @JsonProperty("lastLogMessage") Message lastLogMessage,
			final @JsonProperty("lastLogTime") Timestamp lastLogTime) {
		
		this.sourceDataset = sourceDataset;
		this.datasetCount = datasetCount;
		this.lastLogMessage = lastLogMessage;
		this.lastLogTime = lastLogTime;
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
	public Message lastLogMessage () {
		return this.lastLogMessage;
	}
	
	@JsonGetter
	public Timestamp lastLogTime () {
		return this.lastLogTime;
	}
}
