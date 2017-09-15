package nl.idgis.publisher.domain.service;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.metadata.MetadataDocument;

public class RasterDataset extends Dataset {
	
	private static final long serialVersionUID = -4992204989064821241L;

	public RasterDataset(String id, String name, String alternateTitle, String categoryId, Date revisionDate, Set<Log> logs, boolean confidential, boolean metadataConfidential, boolean wmsOnly, MetadataDocument metadata, String physicalName, String refreshFrequency) {
		super(id, name, alternateTitle, categoryId, revisionDate, logs, confidential, metadataConfidential, wmsOnly, metadata, physicalName, refreshFrequency);
	}

	@Override
	public String toString() {
		return "RasterDataset [id=" + id + ", name=" + name + ", alternateTitle=" + alternateTitle + ", categoryId="
				+ categoryId + ", revisionDate=" + revisionDate + ", logs=" + logs + ", confidential=" + confidential
				+ ", metadataConfidential=" + metadataConfidential + ", metadata=" + metadata + ", wmsOnly=" + wmsOnly
				+ ", physicalName=" + physicalName + ", refreshFrequency=" + refreshFrequency + "]";
	}
}
