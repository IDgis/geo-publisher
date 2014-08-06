package nl.idgis.publisher.service.loader.messages;

import java.io.Serializable;

import nl.idgis.publisher.database.messages.ImportJob;

public class SessionStarted implements Serializable {	
	
	private static final long serialVersionUID = -9040326121733489118L;
	
	private final ImportJob importJob;	
	
	public SessionStarted(ImportJob importJob) {
		this.importJob = importJob;		
	}

	public ImportJob getImportJob() {
		return importJob;
	}

	@Override
	public String toString() {
		return "SessionStarted [importJob=" + importJob + "]";
	}
}
