package nl.idgis.publisher.domain.job.load;

import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;

public enum ImportNotificationType implements NotificationType {

	SOURCE_COLUMNS_CHANGED;
	
	@Override
	public NotificationResult getResult(String resultName) {
		switch(this) {
			case SOURCE_COLUMNS_CHANGED:
				return ConfirmNotificationResult.valueOf(resultName);
			default:
				throw new UnsupportedOperationException();
		
		}
	}
}
