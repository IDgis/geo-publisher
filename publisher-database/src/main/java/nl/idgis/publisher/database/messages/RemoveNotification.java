package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.job.NotificationType;

public class RemoveNotification extends Query {

	private static final long serialVersionUID = -7852054940831000820L;
	
	private final JobInfo job;
	private final NotificationType<?> notificationType;

	public RemoveNotification(JobInfo job, NotificationType<?> notificationType) {
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
		return "RemoveNotification [job=" + job + ", notificationType="
				+ notificationType + "]";
	}

}
