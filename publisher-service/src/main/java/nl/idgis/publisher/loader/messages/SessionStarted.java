package nl.idgis.publisher.loader.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

import nl.idgis.publisher.job.manager.messages.VectorImportJobInfo;

public class SessionStarted implements Serializable {	
	
	private static final long serialVersionUID = 3261050647784309391L;
	
	private final VectorImportJobInfo importJob;
	private final ActorRef session;
	
	public SessionStarted(VectorImportJobInfo importJob, ActorRef session) {
		this.importJob = importJob;
		this.session = session;
	}

	public VectorImportJobInfo getImportJob() {
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
