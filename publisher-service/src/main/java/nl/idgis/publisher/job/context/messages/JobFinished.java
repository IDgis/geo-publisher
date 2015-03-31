package nl.idgis.publisher.job.context.messages;

import java.io.Serializable;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.job.JobState;

public class JobFinished implements Serializable {

	private static final long serialVersionUID = 6765495253538202183L;

	private final JobInfo jobInfo;
	
	private final JobState jobState;
	
	public JobFinished(JobInfo jobInfo, JobState jobState) {
		this.jobInfo = jobInfo;
		this.jobState = jobState;
	}

	public JobInfo getJobInfo() {
		return jobInfo;
	}

	public JobState getJobState() {
		return jobState;
	}

	@Override
	public String toString() {
		return "JobExecuted [jobInfo=" + jobInfo + ", jobState=" + jobState
				+ "]";
	}
}
