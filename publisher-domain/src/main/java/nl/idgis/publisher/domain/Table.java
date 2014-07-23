package nl.idgis.publisher.domain;

import java.io.Serializable;
import java.util.List;

public class Table implements Serializable {
	
	private static final long serialVersionUID = -8960529307246588421L;
	
	private final String name;
	private final List<Column> columns;
	
	public Table(String name, List<Column> columns) {
		this.name = name;
		this.columns = columns;
	}

	public String getName() {
		return name;
	}

	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String toString() {
		return "Table [name=" + name + ", columns=" + columns + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columns == null) ? 0 : columns.hashCode());
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
		Table other = (Table) obj;
		if (columns == null) {
			if (other.columns != null)
				return false;
		} else if (!columns.equals(other.columns))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
