package nl.idgis.publisher.service.loader.messages;

import java.io.Serializable;

import nl.idgis.publisher.database.messages.ImportJobInfo;

public class SessionFinished implements Serializable {	

	private static final long serialVersionUID = -4490550399994987792L;
	
	private final ImportJobInfo importJob;	
	
	public SessionFinished(ImportJobInfo importJob) {
		this.importJob = importJob;		
	}

	public ImportJobInfo getImportJob() {
		return importJob;
	}

	@Override
	public String toString() {
		return "SessionFinished [importJob=" + importJob + "]";
	}
}
