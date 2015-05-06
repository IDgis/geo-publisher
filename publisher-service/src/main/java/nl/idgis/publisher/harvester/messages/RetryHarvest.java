package nl.idgis.publisher.harvester.messages;

import java.io.Serializable;

import nl.idgis.publisher.job.manager.messages.HarvestJobInfo;

public class RetryHarvest implements Serializable {
	
	private static final long serialVersionUID = 245890598166051548L;
	
	private final HarvestJobInfo jobInfo;
	
	public RetryHarvest(HarvestJobInfo jobInfo) {
		this.jobInfo = jobInfo;
	}
	
	public HarvestJobInfo getJobInfo() {
		return jobInfo;
	}
}
