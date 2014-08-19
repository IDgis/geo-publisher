package nl.idgis.publisher.service.messages;

import java.io.Serializable;

import nl.idgis.publisher.database.messages.JobInfo;

public class ActiveJob implements Serializable {
	
	private static final long serialVersionUID = -1048184784091809629L;
	
	private final JobInfo job;
	private final Progress progress;
	
	public ActiveJob(JobInfo job) {
		this(job, null);
	}
	
	public ActiveJob(JobInfo job, Progress progress) {
		this.job = job;
		this.progress = progress;
	}

	public JobInfo getJob() {
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
