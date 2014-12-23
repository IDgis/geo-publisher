package nl.idgis.publisher.domain.service;

import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.MessageProperties;

public abstract class DatasetLog implements MessageProperties {
	
	private static final long serialVersionUID = 2742502035495391359L;

	@Override
	public EntityType getEntityType() {
		return EntityType.DATASET;
	}

	@Override
	public String getIdentification() {
		return null;
	}

	@Override
	public String getTitle() {
		return null;
	}
}
