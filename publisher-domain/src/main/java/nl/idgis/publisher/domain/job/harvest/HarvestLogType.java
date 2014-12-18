package nl.idgis.publisher.domain.job.harvest;

import nl.idgis.publisher.domain.MessageType;

public enum HarvestLogType implements MessageType<HarvestLog> {
	
	REGISTERED,
	UPDATED;
	
	private HarvestLogType() {
		this(HarvestLog.class);
	}
	
	private HarvestLogType(Class<? extends HarvestLog> contentClass) {
		this.contentClass = contentClass;
	}
	
	private final Class<? extends HarvestLog> contentClass;
	
	public Class<? extends HarvestLog> getContentClass() {
		return contentClass;
	}
}
