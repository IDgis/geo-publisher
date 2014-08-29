package nl.idgis.publisher.domain.job;

import java.io.Serializable;

public class Notification implements Serializable {
	
	private static final long serialVersionUID = 5056361418085474033L;
	
	private final NotificationType<?> type;
	private final NotificationResult result;
	
	public Notification(NotificationType<?> type, NotificationResult result) {
		this.type = type;
		this.result = result;
	}

	public NotificationType<?> getType() {
		return type;
	}

	public NotificationResult getResult() {
		return result;
	}

	@Override
	public String toString() {
		return "Notification [type=" + type + ", result=" + result + "]";
	}	
	
}
