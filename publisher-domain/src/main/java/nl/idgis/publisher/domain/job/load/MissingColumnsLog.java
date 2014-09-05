package nl.idgis.publisher.domain.job.load;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.web.EntityType;

public final class MissingColumnsLog extends ImportLog {

	private static final long serialVersionUID = -2353912459550697380L;
	
	private final Set<Column> columns;

	@JsonCreator
	public MissingColumnsLog(			
			@JsonProperty("datasetIdentification") String datasetIdentification, 
			@JsonProperty("datasetName") String datasetName, 
			@JsonProperty("columns") Set<Column> columns) {
		
		super(EntityType.DATASET, datasetIdentification, datasetName);
		
		this.columns = Collections.unmodifiableSet(columns);
	}
	
	public Set<Column> getColumns() {
		return columns;
	}

	@Override
	public String toString() {
		return "MissingColumnsLog [columns=" + columns + "]";
	}

}
