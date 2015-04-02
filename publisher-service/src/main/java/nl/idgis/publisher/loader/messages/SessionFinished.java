package nl.idgis.publisher.loader.messages;

import java.io.Serializable;

import nl.idgis.publisher.job.manager.messages.VectorImportJobInfo;

public class SessionFinished implements Serializable {	

	private static final long serialVersionUID = -4490550399994987792L;
	
	private final VectorImportJobInfo importJob;	
	
	public SessionFinished(VectorImportJobInfo importJob) {
		this.importJob = importJob;		
	}

	public VectorImportJobInfo getImportJob() {
		return importJob;
	}

	@Override
	public String toString() {
		return "SessionFinished [importJob=" + importJob + "]";
	}
}
