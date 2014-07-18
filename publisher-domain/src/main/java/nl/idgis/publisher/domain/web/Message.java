package nl.idgis.publisher.domain.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.idgis.publisher.domain.MessageType;

public final class Message extends Entity {

	private static final long serialVersionUID = -1485836626413687756L;

	private final MessageType type;
	private final List<Object> values;
	
	@JsonCreator
	public Message (final @JsonProperty("type") MessageType type, final @JsonProperty("values") List<Object> values) {
		this.type = type;
		this.values = values == null ? Collections.emptyList() : new ArrayList<> (values);
	}
	
	@JsonGetter
	public MessageType type () {
		return this.type;
	}
	
	@JsonGetter
	public List<Object> values () {
		return Collections.unmodifiableList (this.values);
	}
}
