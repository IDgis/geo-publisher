package nl.idgis.publisher.domain;


public enum DataSourceStatusType implements StatusType {
	OK;

	@Override
	public StatusCategory statusCategory() {
		return StatusCategory.SUCCESS;
	}
}