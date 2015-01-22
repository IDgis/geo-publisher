package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.job.JobState;

public class UpdateState extends Query {
	
	private static final long serialVersionUID = 4108859495125183484L;

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
