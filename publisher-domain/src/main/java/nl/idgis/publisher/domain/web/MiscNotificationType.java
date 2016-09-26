package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;

public enum MiscNotificationType implements NotificationType<DefaultMessageProperties> {
	TEST;

	@Override
	public Class<? extends DefaultMessageProperties> getContentClass() {
		return DefaultMessageProperties.class;
	}

	@Override
	public NotificationResult getResult(String resultName) {
		return ConfirmNotificationResult.valueOf(resultName);
	}

}
