package nl.idgis.publisher.job.manager.messages;

import java.io.Serializable;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.job.NotificationType;

public class AddNotification implements Serializable {
	
	private static final long serialVersionUID = 2371788579123723067L;

	private final JobInfo job;
	
	private final NotificationType<?> notificationType;

	public AddNotification(JobInfo job, NotificationType<?> notificationType) {
		this.job = job;
		this.notificationType = notificationType;
	}

	public JobInfo getJob() {
		return job;
	}

	public NotificationType<?> getNotificationType() {
		return notificationType;
	}

	@Override
	public String toString() {
		return "AddNotification [job=" + job + ", notificationType="
				+ notificationType + "]";
	}
}
