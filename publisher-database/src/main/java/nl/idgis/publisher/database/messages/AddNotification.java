package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.job.NotificationType;

public class AddNotification extends Query {
	
	private static final long serialVersionUID = -7263842439939540377L;
	
	private final JobInfo job;
	private final NotificationType notificationType;

	public AddNotification(JobInfo job, NotificationType notificationType) {
		this.job = job;
		this.notificationType = notificationType;
	}

	public JobInfo getJob() {
		return job;
	}

	public NotificationType getNotificationType() {
		return notificationType;
	}

	@Override
	public String toString() {
		return "AddNotification [job=" + job + ", notificationType="
				+ notificationType + "]";
	}
}
