package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.StatusType;

public enum DataSourceStatusType implements StatusType {
	OK(StatusCategory.SUCCESS),
	NOT_CONNECTED(StatusCategory.ERROR),
	FAILED(StatusCategory.ERROR),
	ABORTED(StatusCategory.ERROR);
	
	private final StatusCategory statusCategory;
	
	private DataSourceStatusType(StatusCategory statusCategory) {
		this.statusCategory = statusCategory;
	}

	@Override
	public StatusCategory statusCategory() {
		return this.statusCategory;
	}
}