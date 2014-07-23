package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Dataset implements Serializable {

	private static final long serialVersionUID = -230416219187079578L;
	
	private final String id, name;
	private final List<Column> columns;
	
	public Dataset(String id, String name, List<Column> columns) {
		this.id = id;
		this.name = name;
		this.columns = columns;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public List<Column> getColumns() {
		return Collections.unmodifiableList(columns);
	}

	@Override
	public String toString() {
		return "Dataset [id=" + id + ", name=" + name + ", columns=" + columns
				+ "]";
	}	
	
}
