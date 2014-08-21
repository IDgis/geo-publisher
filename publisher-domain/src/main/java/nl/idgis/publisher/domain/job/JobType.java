package nl.idgis.publisher.domain.job;

import nl.idgis.publisher.domain.MessageType;
import nl.idgis.publisher.domain.job.harvest.HarvestLogType;

public enum JobType implements MessageType {
	HARVEST(HarvestLogType.class), IMPORT;
	
	private final Class<? extends MessageType> contentClass;

	private JobType() {
		this(null);
	}
	
	private JobType(Class<? extends MessageType> contentClass) {
		this.contentClass = contentClass;
	}
	
	public Class<? extends MessageType> getContentClass() {
		return contentClass;
	}
}
