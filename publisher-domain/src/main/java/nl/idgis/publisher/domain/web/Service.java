/**
 *
 */
package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

/**
 * Representation of a service (WMS, WFS, WMTS).
 * 
 * @author Rob
 *
 */
public class Service extends Identifiable implements Nameable{
	private static final long serialVersionUID = -4339122844101328594L;

	private final String name;
	private final String title;
	private final String alternateTitle;
	private final String abstractText;
	private final String metadata;
	private final String genericLayerId;
	private final String constantsId;
	private final boolean confidential;
	private final boolean isPublished;

	
	@JsonCreator
	@QueryProjection
	public Service(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String name, 
			final @JsonProperty("") String title, 
			final @JsonProperty("") String alternateTitle, 
			final @JsonProperty("") String abstractText, 
			final @JsonProperty("") String metadata,
			final @JsonProperty("") String genericLayerId,
			final @JsonProperty("") String constantsId,
			final @JsonProperty("confidential") boolean confidential,
			final @JsonProperty("") boolean isPublished
		) {
		super(id);
		this.name = name;
		this.title = title;
		this.alternateTitle = alternateTitle;
		this.abstractText = abstractText;
		this.metadata = metadata;
		this.genericLayerId = genericLayerId;
		this.constantsId = constantsId;
		this.confidential = confidential;
		this.isPublished = isPublished;
	}

	@JsonGetter
	public String name() {
		return name;
	}

	@JsonGetter
	public String title() {
		return title;
	}

	@JsonGetter
	public String alternateTitle() {
		return alternateTitle;
	}

	@JsonGetter
	public String abstractText() {
		return abstractText;
	}

	@JsonGetter
	public String metadata() {
		return metadata;
	}

	public String genericLayerId() {
		return genericLayerId;
	}

	public String constantsId() {
		return constantsId;
	}
	
	@JsonGetter
	public boolean confidential () {
		return this.confidential;
	}
	
	@JsonGetter
	public boolean isPublished() {
		return isPublished;
	}
}
