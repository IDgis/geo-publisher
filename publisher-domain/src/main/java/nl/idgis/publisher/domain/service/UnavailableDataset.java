package nl.idgis.publisher.domain.service;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

public final class UnavailableDataset extends Dataset {

	private static final long serialVersionUID = -3885288497540653870L;

	public UnavailableDataset(String id, String name, String alternateTitle, String categoryId, Date revisionDate, Set<Log> logs) {
		super(id, name, alternateTitle, categoryId, revisionDate, logs);
	}

	@Override
	public String toString() {
		return "UnavailableDataset [id=" + id + ", name=" + name
				+ ", alternateTitle=" + alternateTitle + ", categoryId="
				+ categoryId + ", revisionDate=" + revisionDate + ", logs="
				+ logs + "]";
	}

}
