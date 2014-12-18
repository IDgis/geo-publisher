package nl.idgis.publisher.domain.service;

import nl.idgis.publisher.domain.job.harvest.MetadataField;
import nl.idgis.publisher.domain.job.harvest.MetadataLogType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataLog extends DatasetLog {	

	private static final long serialVersionUID = -3158211911240379838L;
	
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
	public String toString() {
		return "MetadataParsingError [identification=" + getIdentification ()
				+ ", field=" + field + ", error=" + error + ", value=" + value
				+ "]";
	}

	
	
}
