package nl.idgis.publisher.domain;

import java.io.Serializable;

public class Dataset implements Serializable {

	private static final long serialVersionUID = -6901956185305992907L;
	
	private final String id;
	private final Table table;
	
	public Dataset(String id, Table table) {
		this.id = id;
		this.table = table;
	}

	public String getId() {
		return id;
	}

	public Table getTable() {
		return table;
	}

	@Override
	public String toString() {
		return "Dataset [id=" + id + ", table=" + table + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((table == null) ? 0 : table.hashCode());
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
		Dataset other = (Dataset) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (table == null) {
			if (other.table != null)
				return false;
		} else if (!table.equals(other.table))
			return false;
		return true;
	}	
	
}
