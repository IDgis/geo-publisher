package nl.idgis.publisher.domain.service;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Column implements Serializable {

	private static final long serialVersionUID = -131755354136052813L;

	private final String name;
	
	private final Type dataType;
	
	private final String alias;

	@JsonCreator
	public Column(@JsonProperty("name") String name, @JsonProperty("dataType") Type dataType, @JsonProperty("alias") String alias) {
		this.name = name;
		this.dataType = dataType;
		this.alias = alias;
	}
	
	public Column(String name, String dataType, String alias) {
		this(name, Type.valueOf(dataType), alias);
	}

	public String getName() {
		return name;
	}

	public Type getDataType() {
		return dataType;
	}
	
	public String getAlias() {
		return alias;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		if (alias == null) {
			if (other.alias != null)
				return false;
		} else if (!alias.equals(other.alias))
			return false;
		if (dataType != other.dataType)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Column [name=" + name + ", dataType=" + dataType + ", alias=" + alias + "]";
	}
}

