package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;
import java.util.List;

import nl.idgis.publisher.service.geoserver.rest.ServiceSettings;
import nl.idgis.publisher.service.geoserver.rest.WorkspaceSettings;

public class EnsureWorkspace implements Serializable {

	private static final long serialVersionUID = -2616789799155772596L;

	private final String workspaceId, title, abstr, contact, organization, position, addressType,
		address, city, state, zipcode, country, telephone, fax, email;
	
	private final List<String> keywords;
	
	public EnsureWorkspace(String workspaceId, String title, String abstr, List<String> keywords,
		String contact, String organization, String position, String addressType, String address, 
		String city, String state, String zipcode, String country, String telephone, String fax, 
		String email) {
		
		this.workspaceId = workspaceId;
		this.title = title;
		this.abstr = abstr;
		this.keywords = keywords;
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
	
	public String getWorkspaceId() {
		return workspaceId;
	}

	public String getTitle() {
		return title;
	}

	public String getAbstract() {
		return abstr;
	}

	public List<String> getKeywords() {
		return keywords;
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
	
	public ServiceSettings getServiceSettings() {
		return new ServiceSettings(
				title,
				abstr,
				keywords);
	}
	
	public WorkspaceSettings getWorkspaceSettings() {
		return new WorkspaceSettings(contact, organization, position, addressType,
			address, city, state, zipcode, country, telephone, fax, email);
	}

	@Override
	public String toString() {
		return "EnsureWorkspace [workspaceId=" + workspaceId + "]";
	}
}
