package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.MessageType;

public class Message<T extends MessageProperties> implements Serializable {

	private static final long serialVersionUID = -6672761127261970546L;

	private final MessageType<T> messageType;
	
	private final T messageProperties;
	
	public Message(MessageType<T> messageType, T messageProperties) {
		this.messageType = messageType;
		this.messageProperties = messageProperties;
	}

	public MessageType<T> getMessageType() {
		return messageType;
	}

	public T getMessageProperties() {
		return messageProperties;
	}

	@Override
	public String toString() {
		return "Message [messageType=" + messageType + ", messageProperties="
				+ messageProperties + "]";
	}
	
}
