package nl.idgis.publisher.domain.web;

import java.time.LocalDateTime;

import nl.idgis.publisher.domain.NotificationType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Notification extends Identifiable {

	private static final long serialVersionUID = -6668318918440675297L;

	private final NotificationType type;
	private final Message message;
	private final LocalDateTime dateTime;
	
	@JsonCreator
	public Notification (
			final @JsonProperty("id") String id, 
			final @JsonProperty("type") NotificationType type, 
			final @JsonProperty("message") Message message,
			final @JsonProperty("dateTime") LocalDateTime dateTime) {
		
		super (id);
		
		this.type = type;
		this.message = message;
		this.dateTime = dateTime;
	}
	
	@JsonGetter
	public NotificationType type () {
		return this.type;
	}
	
	@JsonGetter
	public Message message () {
		return this.message;
	}
	
	@JsonGetter
	public LocalDateTime dateTime () {
		return this.dateTime;
	}
}
