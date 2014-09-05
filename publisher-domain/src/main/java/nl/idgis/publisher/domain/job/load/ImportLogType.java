package nl.idgis.publisher.domain.job.load;

import nl.idgis.publisher.domain.MessageType;

public enum ImportLogType implements MessageType<ImportLog>{

	MISSING_COLUMNS(MissingColumnsLog.class),
	MISSING_FILTER_COLUMNS(MissingColumnsLog.class);
	
	private final Class<? extends ImportLog> contentClass;
	
	ImportLogType(Class<? extends ImportLog> contentClass) {
		this.contentClass = contentClass;
	}

	@Override
	public Class<? extends ImportLog> getContentClass() { 
		return contentClass;
	}
}
