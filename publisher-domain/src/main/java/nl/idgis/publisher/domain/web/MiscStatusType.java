package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.StatusType;

public enum MiscStatusType implements StatusType {
	TEST;

	@Override
	public StatusCategory statusCategory() {
		return StatusCategory.INFO;
	}

}
