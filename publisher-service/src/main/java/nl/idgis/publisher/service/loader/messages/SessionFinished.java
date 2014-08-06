package nl.idgis.publisher.service.loader.messages;

import java.io.Serializable;

import nl.idgis.publisher.database.messages.ImportJob;

public class SessionFinished implements Serializable {	

	private static final long serialVersionUID = -4490550399994987792L;
	
	private final ImportJob importJob;	
	
	public SessionFinished(ImportJob importJob) {
		this.importJob = importJob;		
	}

	public ImportJob getImportJob() {
		return importJob;
	}

	@Override
	public String toString() {
		return "SessionFinished [importJob=" + importJob + "]";
	}
}
