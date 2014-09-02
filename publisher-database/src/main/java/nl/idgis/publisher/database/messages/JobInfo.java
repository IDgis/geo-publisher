package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.domain.job.JobType;
import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.job.NotificationType;

public class JobInfo implements Serializable {

	private static final long serialVersionUID = 6340217799882050992L;
	
	protected final int id;
	protected final JobType jobType;
	protected final List<Notification> notifications;
	
	public JobInfo(int id, JobType jobType) {
		this(id, jobType, Collections.<Notification>emptyList());
	}
	
	public JobInfo(int id, JobType jobType, List<Notification> notifications) {
		this.id = id;
		this.jobType = jobType;
		this.notifications = notifications;
	}
	
	public int getId() {
		return id;
	}
	
	public JobType getJobType() {
		return jobType;
	}
	
	public List<Notification> getNotifications() {
		return Collections.unmodifiableList(notifications);
	}
	
	public boolean hasNotification(NotificationType<?> type) {
		for(Notification notification : notifications) {
			NotificationType<?> currentType = notification.getType();
			if(currentType.equals(type)) {
				return true;
			}
		}
		
		return false;
	}

	@Override
	public String toString() {
		return "JobInfo [id=" + id + ", jobType=" + jobType + "]";
	}
}