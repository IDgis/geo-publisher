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
	private static final long serialVersionUID = 3470435136806376628L;
	
	private final String identification;
	private final String name;
	private final Boolean inUse;

	
	@JsonCreator
	@QueryProjection
	public ServicePublish(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String identification,
			final @JsonProperty("") String name,
			final @JsonProperty("") Boolean inUse
			) {
		super(id);
		this.identification = identification;
		this.name = name;
		this.inUse = inUse;
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
	public Boolean guaranteedSV() {
		return inUse;
	}


	@Override
	public String toString() {
		return "ServicePublish [identification=" + identification + ", name="
				+ name + ", inUse=" + inUse + "]";
	}


}