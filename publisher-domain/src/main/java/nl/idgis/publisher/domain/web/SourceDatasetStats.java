package nl.idgis.publisher.domain.web;

import java.sql.Timestamp;

import nl.idgis.publisher.domain.service.DatasetLog;
import nl.idgis.publisher.domain.service.DatasetLogType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SourceDatasetStats extends Entity {

	private static final long serialVersionUID = 3620755264710033479L;
	
	private final SourceDataset 	sourceDataset;
	private final long				datasetCount;
	
	private final DatasetLogType	lastLogType;
	private final DatasetLog<?>		lastLogParameters;
	private final Timestamp			lastLogTime;
	
	@JsonCreator
	public SourceDatasetStats (
			final @JsonProperty("sourceDataset") SourceDataset sourceDataset, 
			final @JsonProperty("datasetCount") long datasetCount,
			final @JsonProperty("lastLogType") DatasetLogType lastLogType,
			final @JsonProperty("lastLogParameters") DatasetLog<?> lastLogParameters,
			final @JsonProperty("lastLogTime") Timestamp lastLogTime) {
		
		this.sourceDataset = sourceDataset;
		this.datasetCount = datasetCount;
		this.lastLogType = lastLogType;
		this.lastLogParameters = lastLogParameters;
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
	public DatasetLogType lastLogType () {
		return this.lastLogType;
	}
	
	@JsonGetter
	public DatasetLog<?> lastLogParameters () {
		return this.lastLogParameters;
	}
	
	@JsonGetter
	public Timestamp lastLogTime () {
		return this.lastLogTime;
	}
}
