package nl.idgis.publisher.domain.job.load;

import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.NotificationProperties;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;

public enum ImportNotificationType implements NotificationType<NotificationProperties> {

	SOURCE_COLUMNS_CHANGED,
	SOURCE_COLUMNS_CHANGED_ACCEPTED;
	
	@Override
	public NotificationResult getResult(String resultName) {
		switch(this) {
			case SOURCE_COLUMNS_CHANGED:
			case SOURCE_COLUMNS_CHANGED_ACCEPTED:
				return ConfirmNotificationResult.valueOf(resultName);
			default:
				throw new UnsupportedOperationException();
		
		}
	}

	@Override
	public Class<? extends NotificationProperties> getContentClass () {
		return NotificationProperties.class;
	}
}
