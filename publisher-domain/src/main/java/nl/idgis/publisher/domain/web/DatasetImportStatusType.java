package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.StatusType;
import nl.idgis.publisher.domain.StatusType.StatusCategory;

public enum DatasetImportStatusType implements StatusType {
	NOT_IMPORTED (StatusCategory.INFO),
	IMPORTING (StatusCategory.INFO),
	IMPORTED (StatusCategory.SUCCESS),
	IMPORT_FAILED (StatusCategory.ERROR),
	IMPORT_ABORTED(StatusCategory.ERROR);
	
	private final StatusCategory category;
	
	DatasetImportStatusType (final StatusCategory category) {
		this.category = category;
	}

	@Override
	public StatusCategory statusCategory () {
		return category;
	}
}
