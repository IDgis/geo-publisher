package nl.idgis.publisher.service.messages;

import java.io.Serializable;

import nl.idgis.publisher.database.messages.Job;

public class ActiveJob implements Serializable {
	
	private static final long serialVersionUID = -1048184784091809629L;
	
	private final Job job;
	private final Progress progress;
	
	public ActiveJob(Job job) {
		this(job, null);
	}
	
	public ActiveJob(Job job, Progress progress) {
		this.job = job;
		this.progress = progress;
	}

	public Job getJob() {
		return job;
	}

	public Progress getProgress() {
		return progress;
	}

	@Override
	public String toString() {
		return "ActiveJob [job=" + job + ", progress=" + progress + "]";
	}
}
