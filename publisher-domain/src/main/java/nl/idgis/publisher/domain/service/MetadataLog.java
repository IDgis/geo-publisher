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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((error == null) ? 0 : error.hashCode());
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetadataLog other = (MetadataLog) obj;
		if (error != other.error)
			return false;
		if (field != other.field)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MetadataLog [field=" + field + ", error=" + error + ", value="
				+ value + "]";
	}	
}
