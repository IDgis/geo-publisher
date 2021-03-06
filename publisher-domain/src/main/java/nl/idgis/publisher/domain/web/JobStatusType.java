package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.StatusType;

public enum JobStatusType implements StatusType {
	OK(StatusCategory.SUCCESS),
	PLANNED(StatusCategory.INFO),
	RUNNING(StatusCategory.INFO),
	NOT_CONNECTED(StatusCategory.ERROR),
	FAILED(StatusCategory.ERROR),
	ABORTED(StatusCategory.ERROR);
	
	private final StatusCategory statusCategory;
	
	private JobStatusType(StatusCategory statusCategory) {
		this.statusCategory = statusCategory;
	}

	@Override
	public StatusCategory statusCategory() {
		return this.statusCategory;
	}
}