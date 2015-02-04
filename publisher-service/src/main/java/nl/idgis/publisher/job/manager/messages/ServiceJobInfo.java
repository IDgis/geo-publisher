package nl.idgis.publisher.job.manager.messages;

import com.mysema.query.annotations.QueryProjection;

import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.domain.job.JobType;

public class ServiceJobInfo extends JobInfo {	

	private static final long serialVersionUID = 2177579863192957269L;
	
	private final String serviceId;

	@QueryProjection
	public ServiceJobInfo(int id, String serviceId) {
		super(id, JobType.SERVICE);
		
		this.serviceId = serviceId;
	}

	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "ServiceJobInfo [serviceId=" + serviceId + "]";
	}
	
}
