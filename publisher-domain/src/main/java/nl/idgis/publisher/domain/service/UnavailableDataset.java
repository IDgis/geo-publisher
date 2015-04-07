package nl.idgis.publisher.domain.service;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

public final class UnavailableDataset extends Dataset {	

	private static final long serialVersionUID = -1996780762773905048L;

	public UnavailableDataset(String id, String name, String alternateTitle, String categoryId, Date revisionDate, Set<Log> logs, boolean confidential) {
		super(id, name, alternateTitle, categoryId, revisionDate, logs, confidential);
	}

	@Override
	public String toString() {
		return "UnavailableDataset [id=" + id + ", name=" + name
				+ ", alternateTitle=" + alternateTitle + ", categoryId="
				+ categoryId + ", revisionDate=" + revisionDate + ", logs="
				+ logs + ", confidential=" + confidential + "]";
	}	

}
