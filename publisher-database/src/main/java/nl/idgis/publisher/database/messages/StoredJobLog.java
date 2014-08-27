package nl.idgis.publisher.database.messages;

import java.sql.Timestamp;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.MessageType;
import nl.idgis.publisher.domain.job.JobLog;
import nl.idgis.publisher.domain.job.LogLevel;

public class StoredJobLog extends JobLog {
	
	private static final long serialVersionUID = -4960636730010087539L;
	
	private final JobInfo job;
	private final Timestamp when;

	public StoredJobLog(JobInfo job, LogLevel level, MessageType<?> type, final Timestamp when, MessageProperties content) {
		super(level, type, content);

		this.when = when;
		this.job = job;
	}
	
	public JobInfo getJob() {
		return job;
	}

	public Timestamp getWhen () {
		return when;
	}
	
	@Override
	public String toString() {
		return "StoredJobLog [job=" + job + ", level=" + level + ", type="
				+ type + ", content=" + content + "]";
	}

}
