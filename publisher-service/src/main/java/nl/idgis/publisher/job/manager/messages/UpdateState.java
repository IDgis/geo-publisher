package nl.idgis.publisher.job.manager.messages;

import java.io.Serializable;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.job.JobState;

public class UpdateState implements Serializable {

	private static final long serialVersionUID = 6163260840249282877L;

	private final JobInfo job;
	
	private final JobState state;
	
	public UpdateState(JobInfo job, JobState state) {
		this.job = job;
		this.state = state;
	}

	public JobInfo getJob() {
		return job;
	}

	public JobState getState() {
		return state;
	}

	@Override
	public String toString() {
		return "UpdateState [job=" + job + ", state=" + state + "]";
	}
}
