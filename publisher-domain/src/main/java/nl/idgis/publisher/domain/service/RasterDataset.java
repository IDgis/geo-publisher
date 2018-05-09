package nl.idgis.publisher.domain.service;

import java.time.ZonedDateTime;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.metadata.MetadataDocument;

public class RasterDataset extends Dataset {
	
	private static final long serialVersionUID = -4101540833973433022L;
	
	public RasterDataset(String id, String name, String alternateTitle, String categoryId, ZonedDateTime revisionDate, Set<Log> logs, boolean confidential, boolean metadataConfidential, boolean wmsOnly, MetadataDocument metadata, String physicalName, String refreshFrequency) {
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
