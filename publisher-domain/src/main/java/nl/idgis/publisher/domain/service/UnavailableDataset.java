package nl.idgis.publisher.domain.service;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.metadata.MetadataDocument;

public final class UnavailableDataset extends Dataset {
	
	private static final long serialVersionUID = 5352356221150978597L;

	public UnavailableDataset(String id, String name, String alternateTitle, String categoryId, Date revisionDate, Set<Log> logs, boolean confidential, boolean metadataConfidential, boolean wmsOnly, MetadataDocument metadata, String physicalName, String refreshFrequency) {
		super(id, name, alternateTitle, categoryId, revisionDate, logs, confidential, metadataConfidential, wmsOnly, metadata, physicalName, refreshFrequency);
	}

	@Override
	public String toString() {
		return "UnavailableDataset [id=" + id + ", name=" + name + ", alternateTitle=" + alternateTitle
				+ ", categoryId=" + categoryId + ", revisionDate=" + revisionDate + ", logs=" + logs + ", confidential="
				+ confidential + ", metadataConfidential=" + metadataConfidential + ", metadata=" + metadata
				+ ", wmsOnly=" + wmsOnly + ", physicalName=" + physicalName + ", refreshFrequency=" 
				+ refreshFrequency + "]";
	}
	
}
