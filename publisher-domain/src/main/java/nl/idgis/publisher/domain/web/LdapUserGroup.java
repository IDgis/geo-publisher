package nl.idgis.publisher.domain.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

/**
 * Representation of user defined ldapUserGroup.
 * 
 * @author Sandro
 *
 */
public class LdapUserGroup extends Identifiable {
	private static final long serialVersionUID = -3654659870547470085L;
	
	private final String name;
	private final List<String> members;
	
	@JsonCreator
	@QueryProjection
	public LdapUserGroup(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String name, 
			final @JsonProperty("") List<String> members) {
		super(id);
		
		this.name = name;
		this.members = members;
	}
	
	@JsonGetter
	public String name() {
		return name;
	}
	
	@JsonGetter
	public List<String> members() {
		return members;
	}
}