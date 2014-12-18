package nl.idgis.publisher.domain.service;

import nl.idgis.publisher.domain.MessageType;

public enum DatasetLogType implements MessageType<DatasetLog> {
	
	METADATA_PARSING_ERROR(MetadataLog.class),

	UNKNOWN_TABLE,
	TABLE_NOT_FOUND(DatabaseLog.class);
	
	private DatasetLogType() {
		this(DatasetLog.class);
	}
	
	private DatasetLogType(Class<? extends DatasetLog> contentClass) {
		this.contentClass = contentClass;
	}
	
	private final Class<? extends DatasetLog> contentClass;

	@Override
	public Class<? extends DatasetLog> getContentClass() {
		return contentClass;
	}
	
}
