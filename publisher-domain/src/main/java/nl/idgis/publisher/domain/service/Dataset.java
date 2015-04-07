package nl.idgis.publisher.domain.service;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

public abstract class Dataset implements Serializable {

	private static final long serialVersionUID = -8161951034272334261L;

	protected final String id, name, alternateTitle;
	
	protected final String categoryId;
	
	protected final Date revisionDate;
	
	protected final Set<Log> logs;
	
	protected final boolean confidential;
	
	Dataset(String id, String name, String alternateTitle, String categoryId, Date revisionDate, Set<Log> logs, boolean confidential) {
		this.id = id;
		this.name = name;
		this.alternateTitle = alternateTitle;
		this.categoryId = categoryId;
		this.revisionDate = revisionDate;
		this.logs = logs;
		this.confidential = confidential;
	}

	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getAlternateTitle() {
		return alternateTitle;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public Date getRevisionDate() {
		return revisionDate;
	}
	
	public Set<Log> getLogs() {
		return logs;
	}
	
	public boolean isConfidential() {
		return confidential;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((alternateTitle == null) ? 0 : alternateTitle.hashCode());
		result = prime * result
				+ ((categoryId == null) ? 0 : categoryId.hashCode());
		result = prime * result + (confidential ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((logs == null) ? 0 : logs.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		if (alternateTitle == null) {
			if (other.alternateTitle != null)
				return false;
		} else if (!alternateTitle.equals(other.alternateTitle))
			return false;
		if (categoryId == null) {
			if (other.categoryId != null)
				return false;
		} else if (!categoryId.equals(other.categoryId))
			return false;
		if (confidential != other.confidential)
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (revisionDate == null) {
			if (other.revisionDate != null)
				return false;
		} else if (!revisionDate.equals(other.revisionDate))
			return false;
		return true;
	}	

}
