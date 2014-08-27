package nl.idgis.publisher.domain.web;

import java.sql.Timestamp;

import nl.idgis.publisher.domain.StatusType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Status extends Entity {

	private static final long serialVersionUID = -1318543837985040549L;
	
	private final StatusType type;
	private final Timestamp since;
	
	@JsonCreator
	public Status (final @JsonProperty("type") StatusType type, final @JsonProperty("since") Timestamp since) {
		this.type = type;
		this.since = since;
	}
	
	@JsonGetter
	public StatusType type () {
		return this.type;
	}
	
	
	@JsonGetter
	public Timestamp since () {
		return this.since; 
	}
}
