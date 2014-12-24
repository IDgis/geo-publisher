package nl.idgis.publisher.domain.service;

import nl.idgis.publisher.domain.job.harvest.MetadataField;
import nl.idgis.publisher.domain.job.harvest.MetadataLogType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataLog extends DatasetLog<MetadataLog> {
	
	private static final long serialVersionUID = -6645875994758972485L;
	
	private final MetadataField field;
	
	private final MetadataLogType error;
	
	private final Object value;
	
	@JsonCreator
	public MetadataLog(			
			@JsonProperty("field") MetadataField field, 
			@JsonProperty("error") MetadataLogType error, 
			@JsonProperty("value") Object value) {
		
		this.field = field;
		this.error = error;
		this.value = value;
	}
	
	private MetadataLog(Dataset dataset, MetadataField field, MetadataLogType error, Object value) {
		super(dataset);
		
		this.field = field;
		this.error = error;
		this.value = value;
	}
	
	public MetadataField getField() {
		return field;
	}
	
	public MetadataLogType getError() {
		return error;
	}

	public Object getValue() {
		return value;
	}
	
	@Override
	public MetadataLog withDataset(Dataset dataset) {
		return new MetadataLog(dataset, field, error, value);
	}	

	@Override
	public String toString() {
		return "MetadataLog [field=" + field + ", error=" + error + ", value="
				+ value + "]";
	}	
}
