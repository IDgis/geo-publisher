package nl.idgis.publisher.domain.job.harvest;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataParsingError implements Serializable {	

	private static final long serialVersionUID = -3158211911240379838L;
	
	private final String identification;
	private final MetadataField field;
	private final MetadataError error;
	private final Object value;
	
	@JsonCreator
	public MetadataParsingError(
			@JsonProperty("identification") String identification, 
			@JsonProperty("field") MetadataField field, 
			@JsonProperty("error") MetadataError error, 
			@JsonProperty("value") Object value) {
		
		this.identification = identification;
		this.field = field;
		this.error = error;
		this.value = value;
	}
	
	public String getIdentification() {
		return identification;
	}

	public MetadataField getField() {
		return field;
	}
	
	public MetadataError getError() {
		return error;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "MetadataParsingError [identification=" + identification
				+ ", field=" + field + ", error=" + error + ", value=" + value
				+ "]";
	}

	
	
}
