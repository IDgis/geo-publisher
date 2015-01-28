/**
 *
 */
package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

/**
 * @author Rob
 *
 */
public class Style extends Identifiable {
	private static final long serialVersionUID = -103047524556298813L;
	private final String name;
	private final String format;
	private final String version;
	private final String definition;

	@JsonCreator
	@QueryProjection
	public Style(final @JsonProperty("id") String id, final @JsonProperty("name") String name,
			final @JsonProperty("format") String format, final @JsonProperty("version") String version,
			final @JsonProperty("definition") String definition) {
		super(id);
		this.name = name;
		this.format = format;
		this.version = version;
		this.definition = definition;
	}

	@JsonGetter
	public String name() {
		return name;
	}

	@JsonGetter
	public String format() {
		return format;
	}

	@JsonGetter
	public String version() {
		return version;
	}

	@JsonGetter
	public String definition() {
		return definition;
	}
}