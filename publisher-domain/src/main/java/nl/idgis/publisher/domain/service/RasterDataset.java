package nl.idgis.publisher.domain.service;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.metadata.MetadataDocument;

public class RasterDataset extends Dataset {

	private static final long serialVersionUID = -4983546558192495562L;

	public RasterDataset(String id, String name, String alternateTitle, String categoryId, Date revisionDate, Set<Log> logs, boolean confidential, boolean metadataConfidential, MetadataDocument metadata) {
		super(id, name, alternateTitle, categoryId, revisionDate, logs, confidential, metadataConfidential, metadata);		
	}

	@Override
	public String toString() {
		return "RasterDataset [id=" + id + ", name=" + name + ", alternateTitle=" + alternateTitle + ", categoryId="
				+ categoryId + ", revisionDate=" + revisionDate + ", logs=" + logs + ", confidential=" + confidential
				+ ", metadataConfidential=" + metadataConfidential + ", metadata=" + metadata + "]";
	}
}
