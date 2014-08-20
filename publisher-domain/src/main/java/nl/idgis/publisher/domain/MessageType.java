package nl.idgis.publisher.domain;

import java.io.Serializable;

/**
 * Base interface used for log-messages. Should be implemented by every enum
 * in the domain that provides translatable messages.
 */
public interface MessageType extends Serializable {

	public String name();
	public Class<?> getContentClass();
}