package nl.idgis.publisher.service.loader.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.messages.ImportJobInfo;

public class SessionStarted implements Serializable {	
	
	private static final long serialVersionUID = 3261050647784309391L;
	
	private final ImportJobInfo importJob;
	private final ActorRef session;
	
	public SessionStarted(ImportJobInfo importJob, ActorRef session) {
		this.importJob = importJob;
		this.session = session;
	}

	public ImportJobInfo getImportJob() {
		return importJob;
	}
	
	public ActorRef getSession() {
		return session;
	}

	@Override
	public String toString() {
		return "SessionStarted [importJob=" + importJob + ", session="
				+ session + "]";
	}
	
}
