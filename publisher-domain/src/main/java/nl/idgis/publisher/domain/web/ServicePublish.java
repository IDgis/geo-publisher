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
	
	private final Boolean guaranteedSV;
	private final Boolean publicSV;
	private final Boolean secureSV;

	
	@JsonCreator
	@QueryProjection
	public ServicePublish(
			final @JsonProperty("") String id, 
			final @JsonProperty("") Boolean guaranteedSV, 
			final @JsonProperty("") Boolean publicSV, 
			final @JsonProperty("") Boolean secureSV
			) {
		super(id);
		this.guaranteedSV = guaranteedSV;
		this.publicSV = publicSV;
		this.secureSV = secureSV;
	}

	@JsonGetter
	public Boolean guaranteedSV() {
		return guaranteedSV;
	}

	@JsonGetter
	public Boolean publicSV() {
		return publicSV;
	}

	@JsonGetter
	public Boolean secureSV() {
		return secureSV;
	}


}