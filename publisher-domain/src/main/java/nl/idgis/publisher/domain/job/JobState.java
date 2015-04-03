package nl.idgis.publisher.domain.job;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import nl.idgis.publisher.domain.StatusType;

public enum JobState implements StatusType {

	STARTED(StatusType.StatusCategory.INFO, false),
	SUCCEEDED(StatusType.StatusCategory.SUCCESS, true),
	FAILED(StatusType.StatusCategory.ERROR, true),
	ABORTED(StatusType.StatusCategory.ERROR, true);

	private final StatusCategory statusCategory;
	private final boolean isFinished;
	
	JobState(final StatusCategory statusCategory, boolean isFinished) {
		this.statusCategory = statusCategory;
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

	@Override
	public StatusCategory statusCategory () {
		return statusCategory;
	}
}
