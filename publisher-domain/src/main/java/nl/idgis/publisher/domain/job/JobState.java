package nl.idgis.publisher.domain.job;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum JobState {

	STARTED(false),
	SUCCEEDED(true),
	FAILED(true),
	ABORTED(true);
	
	private final boolean isFinished;
	
	JobState(boolean isFinished) {
		this.isFinished = isFinished;
	}
	
	public boolean isFinished() {
		return isFinished;
	}
	
	private final static Set<JobState> finished = buildFinished();
	
	private static Set<JobState> buildFinished() {
		HashSet<JobState> finished = new HashSet<>();
		
		for(JobState jobState : JobState.values()) {
			if(jobState.isFinished) {
				finished.add(jobState);
			}
		}
		
		return Collections.unmodifiableSet(finished);
	}
	
	public static Set<JobState> getFinished() {
		return finished;
	}
}
