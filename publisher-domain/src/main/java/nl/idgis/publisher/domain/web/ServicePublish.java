/**
 *
 */
package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

/**
 @author Sandro
 *
 */
public class ServicePublish extends Identifiable {
	
	private static final long serialVersionUID = 6096502661515317000L;
	
	private final String identification;
	private final String name;
	private final Boolean inUse;
	private final boolean confidential;
	private final boolean wmsOnly;
	
	
	@JsonCreator
	@QueryProjection
	public ServicePublish(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String identification,
			final @JsonProperty("") String name,
			final @JsonProperty("") Boolean inUse,
			final @JsonProperty("confidential") boolean confidential,
			final @JsonProperty("wmsOnly") boolean wmsOnly
			) {
		super(id);
		this.identification = identification;
		this.name = name;
		this.inUse = inUse;
		this.confidential = confidential;
		this.wmsOnly = wmsOnly;
	}


	@JsonGetter
	public String identification() {
		return identification;
	}
	
	@JsonGetter
	public String name() {
		return name;
	}
	
	@JsonGetter
	public Boolean inUse() {
		return inUse;
	}

	@JsonGetter
	public boolean confidential () {
		return confidential;
	}

	@JsonGetter
	public boolean wmsOnly () {
		return wmsOnly;
	}

	@Override
	public String toString() {
		return "ServicePublish [identification=" + identification + ", name="
				+ name + ", inUse=" + inUse + ", confidential=" + confidential
				+ ", wmsOnly=" + wmsOnly + "]";
	}


}