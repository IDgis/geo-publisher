package nl.idgis.publisher.domain.job;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.MessageType;

public interface NotificationType<T extends MessageProperties> extends MessageType<T> {

	String name();

	NotificationResult getResult(String resultName);
}
