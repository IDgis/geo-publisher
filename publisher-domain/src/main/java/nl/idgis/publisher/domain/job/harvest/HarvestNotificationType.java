package nl.idgis.publisher.domain.job.harvest;

import nl.idgis.publisher.domain.job.NotificationProperties;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;

public enum HarvestNotificationType implements NotificationType<NotificationProperties> {
	
	NEW_SOURCE_DATASET;
	
	@Override
	public NotificationResult getResult(String resultName) {
		throw new UnsupportedOperationException(); 
	}

	@Override
	public Class<? extends NotificationProperties> getContentClass () {
		return NotificationProperties.class;
	}
}
