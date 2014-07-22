package nl.idgis.publisher.domain.web;

import org.joda.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class LogItem extends Identifiable {

	private static final long serialVersionUID = -8821414588320986892L;
	
	private final Message message;
	private final LocalDateTime dateTime;
	
	@JsonCreator
	public LogItem (final @JsonProperty("id") String id, final @JsonProperty("message") Message message, final @JsonProperty("dateTime") LocalDateTime dateTime) {
		super (id);
		
		this.message = message;
		this.dateTime = dateTime;
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
