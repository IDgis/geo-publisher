package nl.idgis.publisher.database.messages;

import java.io.Serializable;

import com.mysema.query.annotations.QueryProjection;

public class DatasetInfo implements Serializable {

	private static final long serialVersionUID = 1483600283295264723L;
	
	private final String id, name;
	private String sourceDatasetId, sourceDatasetName;
	private String categoryId, categoryName;

	@QueryProjection
	public DatasetInfo(String id, String name, String sourceDatasetId,
			String sourceDatasetName, String categoryId, String categoryName) {
		super();
		this.id = id;
		this.name = name;
		this.sourceDatasetId = sourceDatasetId;
		this.sourceDatasetName = sourceDatasetName;
		this.categoryId = categoryId;
		this.categoryName = categoryName;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}

	public String getSourceDatasetName() {
		return sourceDatasetName;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	@Override
	public String toString() {
		return "DatasetInfo [id=" + id + ", name=" + name
				+ ", sourceDatasetId=" + sourceDatasetId
				+ ", sourceDatasetName=" + sourceDatasetName + ", categoryId="
				+ categoryId + ", categoryName=" + categoryName + "]";
	}

}
