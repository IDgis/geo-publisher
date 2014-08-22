package nl.idgis.publisher.domain.service;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Column implements Serializable {
	
	private static final long serialVersionUID = 6110525555358536529L;
	
	private final String name;
	private final Type dataType;

	@JsonCreator
	public Column(@JsonProperty("name") String name, @JsonProperty("dataType") Type dataType) {
		this.name = name;
		this.dataType = dataType;
	}
	
	public Column(String name, String dataType) {
		this(name, Type.valueOf(dataType));		
	}

	public String getName() {
		return name;
	}

	public Type getDataType() {
		return dataType;
	}

	@Override
	public String toString() {
		return "Column [name=" + name + ", dataType=" + dataType + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
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
		Column other = (Column) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (dataType == null) {
			if (other.dataType != null)
				return false;
		} else if (!dataType.equals(other.dataType))
			return false;
		return true;
	}
	
}

