package nl.idgis.publisher.domain.service;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

public abstract class Dataset implements Serializable {

	private static final long serialVersionUID = 8936394880173140827L;

	protected final String id;
	
	protected final String categoryId;
	
	protected final Date revisionDate;
	
	protected final Set<Log> logs;
	
	Dataset(String id, String categoryId, Date revisionDate, Set<Log> logs) {
		this.id = id;
		this.categoryId = categoryId;
		this.revisionDate = revisionDate;
		this.logs = logs;
	}

	public String getId() {
		return id;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public Date getRevisionDate() {
		return revisionDate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((categoryId == null) ? 0 : categoryId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((logs == null) ? 0 : logs.hashCode());
		result = prime * result
				+ ((revisionDate == null) ? 0 : revisionDate.hashCode());
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
		if (logs == null) {
			if (other.logs != null)
				return false;
		} else if (!logs.equals(other.logs))
			return false;
		if (revisionDate == null) {
			if (other.revisionDate != null)
				return false;
		} else if (!revisionDate.equals(other.revisionDate))
			return false;
		return true;
	}	

}
