package nl.idgis.publisher.service.geoserver.rest;

public class WorkspaceSettings {

	private final String contact, organization, position, addressType,
		address, city, state, zipcode, country, telephone, fax, email;
	
	public WorkspaceSettings(String contact, String organization, String position, String addressType,
		String address, String city, String state, String zipcode, String country, String telephone, 
		String fax, String email) {
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

	public String getContact() {
		return contact;
	}

	public String getOrganization() {
		return organization;
	}

	public String getPosition() {
		return position;
	}

	public String getAddressType() {
		return addressType;
	}

	public String getAddress() {
		return address;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}

	public String getZipcode() {
		return zipcode;
	}

	public String getCountry() {
		return country;
	}

	public String getTelephone() {
		return telephone;
	}

	public String getFax() {
		return fax;
	}

	public String getEmail() {
		return email;
	}
	
	@Override
	public String toString() {
		return "WorkspaceSettings [contact=" + contact + ", organization="
				+ organization + ", position=" + position + ", addressType="
				+ addressType + ", address=" + address + ", city=" + city
				+ ", state=" + state + ", zipcode=" + zipcode + ", country="
				+ country + ", telephone=" + telephone + ", fax=" + fax
				+ ", email=" + email + "]";
	}
}
