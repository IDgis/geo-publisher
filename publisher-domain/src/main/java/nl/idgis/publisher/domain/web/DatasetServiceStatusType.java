package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.StatusType;

public enum DatasetServiceStatusType implements StatusType {
	NOT_VERIFIED (StatusCategory.INFO),
	VERIFIED (StatusCategory.SUCCESS),
	VERIFY_FAILED (StatusCategory.ERROR),
	ADDED (StatusCategory.SUCCESS),
	ADD_FAILED (StatusCategory.ERROR);	
	
	private final StatusCategory category;
	
	DatasetServiceStatusType (final StatusCategory category) {
		this.category = category;
	}

	@Override
	public StatusCategory statusCategory () {
		return category;
	}
}
