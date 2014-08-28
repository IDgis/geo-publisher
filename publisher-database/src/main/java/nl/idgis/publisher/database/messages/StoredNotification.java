package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;

public class StoredNotification extends Notification {
	private static final long serialVersionUID = -6243644194279579040L;
	
	private final long id;
	private final JobInfo job;
	private final BaseDatasetInfo dataset;
	
	public StoredNotification (
			final long id, 
			final NotificationType<?> type, 
			final NotificationResult result, 
			final JobInfo job,
			final BaseDatasetInfo dataset) {
		super(type, result);
		
		this.id = id;
		this.job = job;
		this.dataset = dataset;
	}

	public long getId () {
		return id;
	}

	public JobInfo getJob () {
		return job;
	}
	
	public BaseDatasetInfo getDataset () {
		return dataset;
	}
}
