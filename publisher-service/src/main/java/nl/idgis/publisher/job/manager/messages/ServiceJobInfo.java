package nl.idgis.publisher.job.manager.messages;

import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.domain.job.JobType;

public abstract class ServiceJobInfo extends JobInfo {

	private static final long serialVersionUID = -6377245672307218086L;

	public ServiceJobInfo(int id) {
		super(id, JobType.SERVICE);
	}
}
