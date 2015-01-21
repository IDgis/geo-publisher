package nl.idgis.publisher.domain.service;

import java.io.Serializable;
import java.util.List;

public class Table implements Serializable {

	private static final long serialVersionUID = -3551230988426854409L;
	
	private final List<Column> columns;
	
	public Table(List<Column> columns) {		
		this.columns = columns;
	}	

	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String toString() {
		return "Table [columns=" + columns + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columns == null) ? 0 : columns.hashCode());		
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
		Table other = (Table) obj;
		if (columns == null) {
			if (other.columns != null)
				return false;
		} else if (!columns.equals(other.columns))
			return false;		
		return true;
	}
}
