package nl.idgis.publisher.domain.job;

public interface NotificationType {

	String name();

	NotificationResult getResult(String resultName);
}
