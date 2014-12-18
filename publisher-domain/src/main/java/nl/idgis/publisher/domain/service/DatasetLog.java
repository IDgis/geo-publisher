package nl.idgis.publisher.domain.service;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.web.EntityType;

public class DatasetLog extends MessageProperties {
		
	private static final long serialVersionUID = -5077072540251786319L;

	public DatasetLog() {
		super(EntityType.SOURCE_DATASET, null, null);
	}	
}
