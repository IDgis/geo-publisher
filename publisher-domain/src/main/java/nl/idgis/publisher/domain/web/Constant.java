/**
 *
 */
package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mysema.query.annotations.QueryProjection;

/**
 * Representation of user defined constants.
 * 
 * @author Sandro
 *
 */
public class Constant extends Identifiable {
	private static final long serialVersionUID = -103047524556298813L;
	
	private final String contact;
	private final String organization;
	private final String position;
	private final String addressType;
	private final String address;
	private final String city;
	private final String state;
	private final String zipcode;
	private final String country;
	private final String telephone;
	private final String fax;
	private final String email;

	@JsonCreator
	@QueryProjection
	public Constant(
			final @JsonProperty("") String id, 
			final @JsonProperty("") String contact,
			final @JsonProperty("") String organization,
			final @JsonProperty("") String position,
			final @JsonProperty("") String addressType,
			final @JsonProperty("") String address,
			final @JsonProperty("") String city,
			final @JsonProperty("") String state,
			final @JsonProperty("") String zipcode,
			final @JsonProperty("") String country,
			final @JsonProperty("") String telephone,
			final @JsonProperty("") String fax,
			final @JsonProperty("") String email) {
		super(id);
		this.contact = contact;
		this.organization = organization;
		this.position = position;
		this.addressType = addressType;
		this.address = address;
		this.city = city;
		this.state = state;
		this.zipcode = zipcode;
		this.country = country;
		this.telephone = telephone;
		this.fax = fax;
		this.email = email;
	}

	@JsonGetter
	public String contact() {
		return contact;
	}

	@JsonGetter
	public String organization() {
		return organization;
	}

	@JsonGetter
	public String position() {
		return position;
	}

	@JsonGetter
	public String addressType() {
		return addressType;
	}

	@JsonGetter
	public String address() {
		return address;
	}

	@JsonGetter
	public String city() {
		return city;
	}

	@JsonGetter
	public String state() {
		return state;
	}

	@JsonGetter
	public String zipcode() {
		return zipcode;
	}

	@JsonGetter
	public String country() {
		return country;
	}

	@JsonGetter
	public String telephone() {
		return telephone;
	}

	@JsonGetter
	public String fax() {
		return fax;
	}

	@JsonGetter
	public String email() {
		return email;
	}
}