package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;

public class AddNotificationResult extends Query {

	private static final long serialVersionUID = 5755891069181869646L;
	
	private final JobInfo job;
	private final NotificationType<?> notificationType;
	private final NotificationResult notificationResult;

	public AddNotificationResult(JobInfo job, NotificationType<?> notificationType, NotificationResult notificationResult) {
		this.job = job;
		this.notificationType = notificationType;
		this.notificationResult = notificationResult;
	}

	public JobInfo getJob() {
		return job;
	}

	public NotificationType<?> getNotificationType() {
		return notificationType;
	}

	public NotificationResult getNotificationResult() {
		return notificationResult;
	}

	@Override
	public String toString() {
		return "AddNotificationResult [job=" + job + ", notificationType="
				+ notificationType + ", notificationResult="
				+ notificationResult + "]";
	}
}
