package nl.idgis.publisher.domain.job.harvest;

import nl.idgis.publisher.domain.MessageType;

public enum HarvestLogType implements MessageType {
	
	METADATA_PARSING_ERROR(MetadataLog.class),

	UNKNOWN_TABLE,
	TABLE_NOT_FOUND(DatabaseLog.class),
	
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
