package nl.idgis.publisher.job.context.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.job.JobState;

public class UpdateJobState implements Serializable {
	
	private static final long serialVersionUID = 39985678462777825L;
	
	private final JobState state;
	
	public UpdateJobState(JobState state) {
		this.state = state;
	}

	public JobState getState() {
		return state;
	}

	@Override
	public String toString() {
		return "UpdateJobState [state=" + state + "]";
	}
}
