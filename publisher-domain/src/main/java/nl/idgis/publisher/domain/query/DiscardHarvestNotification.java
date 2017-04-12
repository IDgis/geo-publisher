package nl.idgis.publisher.domain.query;

public class DiscardHarvestNotification implements DomainQuery<Boolean> {
	
	private static final long serialVersionUID = 5648470165294406577L;
	
	private final String notificationId;
	private final String sourceDatasetId;

	public DiscardHarvestNotification(String sourceDatasetId, String notificationId) {
		this.notificationId = notificationId;
		this.sourceDatasetId = sourceDatasetId;
	}

	public String notificationId() {
		return notificationId;
	}

	public String sourceDatasetId() {
		return sourceDatasetId;
	}
}
