package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 @author Sandro
 *
 */
public class DatasetPublishedService extends Identifiable {
	
	private static final long serialVersionUID = 1L;
	
	private final String name;
	private final String environmentId;
	
	@JsonCreator
	public DatasetPublishedService(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String name, 
			final @JsonProperty("") String environmentId
			) {
		super(id);
		this.name = name;
		this.environmentId = environmentId;
	}
	
	@JsonGetter
	public String name() {
		return name;
	}
	
	@JsonGetter
	public String environmentId() {
		return environmentId;
	}

	@Override
	public String toString() {
		return "DatasetPublishedService ["
				+ "name=" + name 
				+ ", environmentId=" + environmentId 
			+ "]";
	}

}