package nl.idgis.publisher.database.messages;

import java.io.Serializable;

import com.mysema.query.annotations.QueryProjection;

public class DataSourceInfo implements Serializable {

	private static final long serialVersionUID = 1483600283295264723L;
	
	private final String id, name;
	
	@QueryProjection
	public DataSourceInfo(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "DataSourceInfo [id=" + id + ", name=" + name + "]";
	}
}
