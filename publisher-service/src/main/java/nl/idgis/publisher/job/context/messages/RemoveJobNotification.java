package nl.idgis.publisher.job.context.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.job.NotificationType;

public class RemoveJobNotification implements Serializable {

	private static final long serialVersionUID = 299441516415583501L;
	
	private final NotificationType<?> notificationType;
	
	public RemoveJobNotification(NotificationType<?> notificationType) {
		this.notificationType = notificationType;
	}
	
	public NotificationType<?> getNotificationType() {
		return notificationType;
	}
	
	@Override
	public String toString() {
		return "RemoveJobNotification [notificationType=" + notificationType + "]";
	}

}
