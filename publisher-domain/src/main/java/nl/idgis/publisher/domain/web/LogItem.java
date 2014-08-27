package nl.idgis.publisher.domain.web;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class LogItem extends Identifiable {

	private static final long serialVersionUID = -8821414588320986892L;
	
	private final Message message;
	private final Timestamp dateTime;
	
	@JsonCreator
	public LogItem (final @JsonProperty("id") String id, final @JsonProperty("message") Message message, final @JsonProperty("dateTime") Timestamp dateTime) {
		super (id);
		
		this.message = message;
		this.dateTime = dateTime;
	}
	
	@JsonGetter
	public Message message () {
		return this.message;
	}
	
	@JsonGetter
	public Timestamp dateTime () {
		return this.dateTime;
	}
}
