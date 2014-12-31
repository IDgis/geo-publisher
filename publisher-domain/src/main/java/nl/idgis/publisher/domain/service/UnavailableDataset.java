package nl.idgis.publisher.domain.service;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

public final class UnavailableDataset extends Dataset {
	
	private static final long serialVersionUID = -30944815577357690L;

	public UnavailableDataset(String id, String categoryId, Date revisionDate, Set<Log> logs) {
		super(id, categoryId, revisionDate, logs);
	}

	@Override
	public String toString() {
		return "UnavailableDataset [id=" + id + ", categoryId=" + categoryId
				+ ", revisionDate=" + revisionDate + "]";
	}

}
