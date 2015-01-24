package nl.idgis.publisher.job.context.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.job.NotificationType;

public class AddJobNotification implements Serializable {

	private static final long serialVersionUID = 942318584767859552L;
	
	private final NotificationType<?> notificationType;
	
	public AddJobNotification(NotificationType<?> notificationType) {
		this.notificationType = notificationType;
	}
	
	public NotificationType<?> getNotificationType() {
		return notificationType;
	}
	
	@Override
	public String toString() {
		return "AddJobNotification [notificationType=" + notificationType + "]";
	}

}
