package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

import com.mysema.query.annotations.QueryProjection;

public class SourceDatasetInfo implements Serializable {

	private static final long serialVersionUID = 1483600283295264723L;
	
	public enum Type {
		VECTOR,
		UNAVAILABLE
	}
	
	private String dataSourceId, dataSourceName;
	private final String id, name;
	private final String categoryId, categoryName;
	private Long count;
	private Type type;

	@QueryProjection
	public SourceDatasetInfo(String id, String name, String dataSourceId,
			String dataSourceName, String categoryId, String categoryName,
			Long count, final String type) {
		super();
		
		if (type == null) {
			throw new NullPointerException ("type cannot be null");
		}
		
		this.id = id;
		this.name = name;
		this.dataSourceId = dataSourceId;
		this.dataSourceName = dataSourceName;
		this.categoryId = categoryId;
		this.categoryName = categoryName;
		this.count = count;
		this.type = Type.valueOf (type);
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

	public String getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public Long getCount() {
		return count;
	}

	public Type getType () {
		return type;
	}

}
