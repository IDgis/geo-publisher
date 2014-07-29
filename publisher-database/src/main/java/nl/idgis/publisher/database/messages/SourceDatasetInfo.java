package nl.idgis.publisher.database.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.web.DataSource;

import com.mysema.query.annotations.QueryProjection;

public class SourceDatasetInfo implements Serializable {

	private static final long serialVersionUID = 1483600283295264723L;
	
	private String dataSourceId, dataSourceName;
	private final String id, name;
	private Integer count;
	
	@QueryProjection
	public SourceDatasetInfo(String dsId, String dsName, String id, String name, Integer count) {
		this.dataSourceId = dsId;
		this.dataSourceName = dsName;
		this.id = id;
		this.name = name;
		this.count = count;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}
	
	public String getDataSourceName() {
		return dataSourceName;
	}
	
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Integer getCount(){
		return count;
	}
	
	@Override
	public String toString() {
		return "SourceDatasetInfo [id=" + id + ", name=" + name + "]";
	}
}
