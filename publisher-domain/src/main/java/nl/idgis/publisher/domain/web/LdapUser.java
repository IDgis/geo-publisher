package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

/**
 * Representation of user defined ldapUser.
 * 
 * @author Sandro
 *
 */
public class LdapUser extends Identifiable {
	private static final long serialVersionUID = -5392205331277635366L;
	
	private final String mail;
	private final String fullName;
	private final String lastName;
	private final String password;
	
	@JsonCreator
	@QueryProjection
	public LdapUser(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String mail, 
			final @JsonProperty("") String fullName, 
			final @JsonProperty("") String lastName, 
			final @JsonProperty("") String password) {
		super(id);
		this.mail = mail;
		this.fullName = fullName;
		this.lastName = lastName;
		this.password = password;
	}
	
	@JsonGetter
	public String mail() {
		return mail;
	}
	
	@JsonGetter
	public String fullName() {
		return fullName;
	}
	
	@JsonGetter
	public String lastName() {
		return lastName;
	}
	
	@JsonGetter
	public String password() {
		return password;
	}
}