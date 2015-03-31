package nl.idgis.publisher.job.manager.messages;

import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.domain.job.JobType;

public abstract class ServiceJobInfo extends JobInfo {	

	private static final long serialVersionUID = 1166819920952131206L;
	
	protected final boolean published;

	public ServiceJobInfo(int id, boolean published) {
		super(id, JobType.SERVICE);
		
		this.published = published;
	}
	
	public boolean isPublished() {
		return published;
	}
}
