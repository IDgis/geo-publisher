package nl.idgis.publisher.domain.job.load;

import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;

public enum ImportNotificationType implements NotificationType<ImportNotificationProperties> {

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

	@Override
	public Class<? extends ImportNotificationProperties> getContentClass () {
		return ImportNotificationProperties.class;
	}
}
