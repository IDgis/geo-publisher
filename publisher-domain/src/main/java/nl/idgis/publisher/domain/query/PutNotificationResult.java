package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.response.Response;

public class PutNotificationResult implements DomainQuery<Response<?>> {
	private static final long serialVersionUID = -6325886135278715974L;
	
	private final String notificationId;
	private final ConfirmNotificationResult result;
	
	public PutNotificationResult (final String notificationId, final ConfirmNotificationResult result) {
		if (notificationId == null) {
			throw new NullPointerException ("notificationId cannot be null");
		}
		if (result == null) {
			throw new NullPointerException ("result cannot be null");
		}
		
		this.notificationId = notificationId;
		this.result = result;
	}
	
	public String notificationId () {
		return notificationId;
	}
	
	public ConfirmNotificationResult result () {
		return result;
	}
}
