package nl.idgis.publisher.domain.job.service;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.MessageType;

public enum ServiceLogType implements MessageType<MessageProperties> {

	VERIFIED,
	ADDED;

	@Override
	public Class<? extends MessageProperties> getContentClass() {
		return null;
	}
	
}
