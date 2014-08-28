package nl.idgis.publisher.domain.notification;

import nl.idgis.publisher.domain.MessageType;

public enum DatasetNotificationType implements MessageType<DatasetNotification> {
	SOURCE_COLUMNS_CHANGED;

	@Override
	public Class<? extends DatasetNotification> getContentClass () {
		return DatasetNotification.class;
	}
}
