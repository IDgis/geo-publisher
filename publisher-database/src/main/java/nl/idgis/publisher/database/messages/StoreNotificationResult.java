package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.job.NotificationResult;

public final class StoreNotificationResult extends Query {
	private static final long serialVersionUID = -7020878883060498783L;
	
	private final int notificationId;
	private final NotificationResult result;
	
	public StoreNotificationResult (final int notificationId, final NotificationResult result) {
		if (result == null) {
			throw new NullPointerException ("result cannot be null");
		}
		
		this.notificationId = notificationId;
		this.result = result;
	}

	public int getNotificationId () {
		return notificationId;
	}

	public NotificationResult getResult () {
		return result;
	}
	
	@Override
	public String toString () {
		return "StoreNotificationResult [notificationId=" + notificationId + ", result=" + result.name () + "]";
	}
}
