package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.MessageType;
import nl.idgis.publisher.domain.job.JobLog;
import nl.idgis.publisher.domain.job.LogLevel;

public class StoredJobLog extends JobLog {
	
	private static final long serialVersionUID = -4960636730010087539L;
	
	private final JobInfo job;

	public StoredJobLog(JobInfo job, LogLevel level, MessageType<?> type, MessageProperties content) {
		super(level, type, content);
		
		this.job = job;
	}
	
	public JobInfo getJob() {
		return job;
	}

	@Override
	public String toString() {
		return "StoredJobLog [job=" + job + ", level=" + level + ", type="
				+ type + ", content=" + content + "]";
	}

}
