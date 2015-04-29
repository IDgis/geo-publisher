package nl.idgis.publisher.domain.service;

import nl.idgis.publisher.domain.MessageType;

public enum DatasetLogType implements MessageType<DatasetLog<?>> {
	
	METADATA_PARSING_ERROR(MetadataLog.class),

	UNKNOWN_TABLE,
	TABLE_NOT_FOUND(DatabaseLog.class),
	
	UNKNOWN_FILE,
	FILE_NOT_FOUND(FileLog.class);
	
	private DatasetLogType() {
		this(null);
	}	
	
	private <T extends DatasetLog<T>> DatasetLogType(Class<T> contentClass) {
		this.contentClass = contentClass;
	}
	
	private final Class<? extends DatasetLog<?>> contentClass;

	@Override
	public Class<? extends DatasetLog<?>> getContentClass() {
		return contentClass;
	}
	
}
