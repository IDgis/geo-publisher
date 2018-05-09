package nl.idgis.publisher.domain.job.harvest;

import nl.idgis.publisher.domain.job.NotificationProperties;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;

public enum HarvestNotificationType implements NotificationType<NotificationProperties> {
	
	NEW_SOURCE_DATASET,
	SOURCE_DATASET_DELETED,
	SOURCE_DATASET_UNAVAILABLE,
	CONFIDENTIAL_CHANGED,
	WMS_ONLY_CHANGED,
	UNKNOWN;
	
	@Override
	public NotificationResult getResult(String resultName) {
		throw new UnsupportedOperationException(); 
	}

	@Override
	public Class<? extends NotificationProperties> getContentClass () {
		return NotificationProperties.class;
	}
	
	public static HarvestNotificationType getHarvestNotificationType(String type) {
		if("NEW_SOURCE_DATASET".equals(type)) {
			return HarvestNotificationType.NEW_SOURCE_DATASET;
		} else if("SOURCE_DATASET_DELETED".equals(type)) {
			return HarvestNotificationType.SOURCE_DATASET_DELETED;
		} else if("SOURCE_DATASET_UNAVAILABLE".equals(type)) {
			return HarvestNotificationType.SOURCE_DATASET_UNAVAILABLE;
		} else if("CONFIDENTIAL_CHANGED".equals(type)) {
			return HarvestNotificationType.CONFIDENTIAL_CHANGED;
		} else if("WMS_ONLY_CHANGED".equals(type)) {
			return HarvestNotificationType.WMS_ONLY_CHANGED;
		} else {
			return HarvestNotificationType.UNKNOWN;
		}
	}
}
