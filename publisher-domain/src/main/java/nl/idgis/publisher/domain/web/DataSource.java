package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class DataSource extends Identifiable {

	private static final long serialVersionUID = -3766591281436677875L;
	
	private final String name;
	private final Status status;
	
	@JsonCreator
	public DataSource (final @JsonProperty("id") String id, final @JsonProperty("name") String name, final @JsonProperty("status") Status status) {
		super (id);
		
		this.name = name;
		this.status = status;
	}
	
	@JsonGetter
	public String name () {
		return this.name;
	}
	
	@JsonGetter
	public Status status () {
		return this.status;
	}
}
