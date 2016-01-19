package nl.idgis.publisher.domain.service;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.metadata.MetadataDocument;

public final class UnavailableDataset extends Dataset {
	
	private static final long serialVersionUID = 6265196080628914796L;

	public UnavailableDataset(String id, String name, String alternateTitle, String categoryId, Date revisionDate, Set<Log> logs, boolean confidential, MetadataDocument metadata) {
		super(id, name, alternateTitle, categoryId, revisionDate, logs, confidential, metadata);
	}

	@Override
	public String toString() {
		return "UnavailableDataset [id=" + id + ", name=" + name
				+ ", alternateTitle=" + alternateTitle + ", categoryId="
				+ categoryId + ", revisionDate=" + revisionDate + ", logs="
				+ logs + ", confidential=" + confidential + "]";
	}	

}
