package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.job.JobState;

public class UpdateJobState extends Query {

	private static final long serialVersionUID = 4158324360734881565L;
	
	private final Job job;
	private final JobState state;
	
	public UpdateJobState(Job job, JobState state) {
		this.job = job;
		this.state = state;
	}

	public Job getJob() {
		return job;
	}

	public JobState getState() {
		return state;
	}

	@Override
	public String toString() {
		return "UpdateJobState [job=" + job + ", state=" + state + "]";
	}
}
