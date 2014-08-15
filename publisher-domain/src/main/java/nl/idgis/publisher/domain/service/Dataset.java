package nl.idgis.publisher.domain.service;

import java.io.Serializable;
import java.util.Date;

public class Dataset implements Serializable {

	private static final long serialVersionUID = -6901956185305992907L;
	
	private final String id, categoryId;	
	private final Table table;
	private final Date revisionDate;
	
	public Dataset(String id, String categoryId, Table table, Date revisionDate) {
		this.id = id;
		this.categoryId = categoryId;
		this.table = table;
		this.revisionDate = revisionDate;
	}

	public String getId() {
		return id;
	}
	
	public String getCategoryId() {
		return categoryId;
	}

	public Table getTable() {
		return table;
	}
	
	public Date getRevisionDate() {
		return revisionDate;
	}

	@Override
	public String toString() {
		return "Dataset [id=" + id + ", categoryId=" + categoryId + ", table="
				+ table + ", revisionDate=" + revisionDate + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((categoryId == null) ? 0 : categoryId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((revisionDate == null) ? 0 : revisionDate.hashCode());
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
		if (categoryId == null) {
			if (other.categoryId != null)
				return false;
		} else if (!categoryId.equals(other.categoryId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (revisionDate == null) {
			if (other.revisionDate != null)
				return false;
		} else if (!revisionDate.equals(other.revisionDate))
			return false;
		if (table == null) {
			if (other.table != null)
				return false;
		} else if (!table.equals(other.table))
			return false;
		return true;
	}
}
