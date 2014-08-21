package nl.idgis.publisher.messages;

import java.io.Serializable;

public class ActiveJobs implements Serializable {
	
	private static final long serialVersionUID = 2459183640295892580L;
	
	private final Iterable<ActiveJob> activeJobs;
	
	public ActiveJobs(Iterable<ActiveJob> activeJobs) {
		this.activeJobs = activeJobs;
	}

	public Iterable<ActiveJob> getActiveJobs() {
		return activeJobs;
	}

	@Override
	public String toString() {
		return "ActiveJobs [activeJobs=" + activeJobs + "]";
	}
	
}
