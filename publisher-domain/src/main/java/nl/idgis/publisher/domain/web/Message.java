package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.MessageType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Message extends Entity {

	private static final long serialVersionUID = -1485836626413687756L;

	private final MessageType<?> type;
	private MessageProperties properties;
	
	@JsonCreator
	public Message (final @JsonProperty("type") MessageType<?> type, final @JsonProperty("properties") MessageProperties properties) {
		this.type = type;
		this.properties = properties;
	}
	
	@JsonGetter
	public MessageType<?> type () {
		return this.type;
	}
	
	@JsonGetter
	public MessageProperties properties () {
		return properties;
	}
}
