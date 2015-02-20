/**
 *
 */
package nl.idgis.publisher.domain.web;

import java.util.List;

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
public class Service extends Identifiable {
	private static final long serialVersionUID = -4339122844101328594L;

	private final String name;
	private final String title;
	private final String alternateTitle;
	private final String abstractText;
	private final List<String> keywords;
	private final String metadata;
	private final String watermark;
	private final Boolean published;
	private final String genericLayerId;
	private final String constantsId;

	
	@JsonCreator
	@QueryProjection
	public Service(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String name, 
			final @JsonProperty("") String title, 
			final @JsonProperty("") String alternateTitle, 
			final @JsonProperty("") String abstractText, 
			final @JsonProperty("") List<String> keywords,
			final @JsonProperty("") String metadata, 
			final @JsonProperty("") String watermark,
			final @JsonProperty("") Boolean published,
			final @JsonProperty("") String genericLayerId,
			final @JsonProperty("") String constantsId
			) {
		super(id);
		this.name = name;
		this.title = title;
		this.alternateTitle = alternateTitle;
		this.abstractText = abstractText;
		this.keywords = keywords;
		this.metadata = metadata;
		this.watermark = watermark;
		this.published = published;
		this.genericLayerId = genericLayerId;
		this.constantsId = constantsId;
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
	public List<String> keywords() {
		return keywords;
	}

	@JsonGetter
	public String metadata() {
		return metadata;
	}

	@JsonGetter
	public String watermark() {
		return watermark;
	}

	@JsonGetter
	public Boolean published() {
		return published;
	}

	public String genericLayerId() {
		return genericLayerId;
	}

	public String constantsId() {
		return constantsId;
	}


}