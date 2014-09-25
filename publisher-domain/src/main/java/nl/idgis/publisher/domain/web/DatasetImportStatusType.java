package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.StatusType;

public enum DatasetImportStatusType implements StatusType {
	NOT_IMPORTED (StatusCategory.INFO),
	IMPORTING (StatusCategory.INFO),
	IMPORTED (StatusCategory.SUCCESS),
	IMPORT_FAILED (StatusCategory.ERROR);
	
	private final StatusCategory category;
	
	DatasetImportStatusType (final StatusCategory category) {
		this.category = category;
	}

	@Override
	public StatusCategory statusCategory () {
		return category;
	}
}
