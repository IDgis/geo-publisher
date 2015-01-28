/**
 * 
 */
package nl.idgis.publisher.admin.messages;

import com.mysema.query.annotations.QueryProjection;

import nl.idgis.publisher.domain.web.Identifiable;

/**
 * 
 * Message information for Style. 
 * @author Rob
 *
 */
public class StyleInfo extends Identifiable {

	private static final long serialVersionUID = 4711070067819521821L;
	
	private final String name;
	private final String format;
	private final String version;
	private final String definition;
	
	@QueryProjection
	public StyleInfo(String id, String name, String format, String version, String definition) {
		super(id);
		this.name = name;
		this.format = format;
		this.version = version;
		this.definition = definition;
	}

	public String getName() {
		return name;
	}

	public String getFormat() {
		return format;
	}

	public String getVersion() {
		return version;
	}

	public String getDefinition() {
		return definition;
	}
	
}
