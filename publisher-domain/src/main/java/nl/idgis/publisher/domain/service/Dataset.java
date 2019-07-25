package nl.idgis.publisher.domain.service;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.metadata.MetadataDocument;

public abstract class Dataset implements Serializable {
	
	private static final long serialVersionUID = 8782715911253428018L;
	
	protected final String id, name, alternateTitle;
	
	protected final String categoryId;
	
	protected final ZonedDateTime revisionDate;
	
	protected final Set<Log> logs;
	
	protected final boolean confidential, metadataConfidential;
	
	protected final MetadataDocument metadata;
	
	protected final boolean wmsOnly;
	
	protected final String physicalName;
	
	protected final String refreshFrequency;
	
	protected final boolean archived;
	
	Dataset(String id, String name, String alternateTitle, String categoryId, ZonedDateTime revisionDate, Set<Log> logs, boolean confidential, boolean metadataConfidential, boolean wmsOnly, MetadataDocument metadata, String physicalName, String refreshFrequency, boolean archived) {
		this.id = id;
		this.name = name;
		this.alternateTitle = alternateTitle;
		this.categoryId = categoryId;
		this.revisionDate = revisionDate;
		this.logs = logs;
		this.confidential = confidential;
		this.metadataConfidential = metadataConfidential;
		this.wmsOnly = wmsOnly;
		this.metadata = metadata;
		this.physicalName = physicalName;
		this.refreshFrequency = refreshFrequency;
		this.archived = archived;
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

	public ZonedDateTime getRevisionDate() {
		return revisionDate;
	}
	
	public Set<Log> getLogs() {
		return logs;
	}
	
	public boolean isConfidential() {
		return confidential;
	}
	
	public boolean isMetadataConfidential() {
		return metadataConfidential;
	}
	
	public boolean isWmsOnly() {
		return wmsOnly;
	}
	
	public String getPhysicalName() {
		return physicalName;
	}
	
	public String getRefreshFrequency() {
		return refreshFrequency;
	}
	
	public boolean isArchived() {
		return archived;
	}

	public Optional<MetadataDocument> getMetadata() {
		return Optional.ofNullable(metadata);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alternateTitle == null) ? 0 : alternateTitle.hashCode());
		result = prime * result + (archived ? 1231 : 1237);
		result = prime * result + ((categoryId == null) ? 0 : categoryId.hashCode());
		result = prime * result + (confidential ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((logs == null) ? 0 : logs.hashCode());
		result = prime * result + (metadataConfidential ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((physicalName == null) ? 0 : physicalName.hashCode());
		result = prime * result + ((refreshFrequency == null) ? 0 : refreshFrequency.hashCode());
		result = prime * result + ((revisionDate == null) ? 0 : revisionDate.hashCode());
		result = prime * result + (wmsOnly ? 1231 : 1237);
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
		if (archived != other.archived)
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
		if (metadataConfidential != other.metadataConfidential)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (physicalName == null) {
			if (other.physicalName != null)
				return false;
		} else if (!physicalName.equals(other.physicalName))
			return false;
		if (refreshFrequency == null) {
			if (other.refreshFrequency != null)
				return false;
		} else if (!refreshFrequency.equals(other.refreshFrequency))
			return false;
		if (revisionDate == null) {
			if (other.revisionDate != null)
				return false;
		} else if (!revisionDate.equals(other.revisionDate))
			return false;
		if (wmsOnly != other.wmsOnly)
			return false;
		return true;
	}	

}
