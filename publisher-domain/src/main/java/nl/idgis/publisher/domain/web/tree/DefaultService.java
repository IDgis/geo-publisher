package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class DefaultService implements Service, Serializable {	

	private static final long serialVersionUID = -4657847579083869249L;

	private final String id, name, title, abstr, contact, organization, position, addressType,
		address, city, state, zipcode, country, telephone, fax, email;
	
	private final List<String> keywords;
	
	private final GroupLayer root;
	
	public DefaultService(String id, String name, String title, String abstr, List<String> keywords, 
			String contact, String organization, String position, String addressType, String address, 
			String city, String state, String zipcode, String country, String telephone, String fax,
			String email, PartialGroupLayer root, List<DefaultVectorDatasetLayer> datasets, List<PartialGroupLayer> groups, 
			List<StructureItem> structure, Map<String, StyleRef> styles) {
		
		this.id = id;
		this.name = name;
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
		this.root = new DefaultGroupLayer(root, datasets, groups, structure, styles);
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getTitle() {
		return title;
	}
	
	@Override
	public String getAbstract() {
		return abstr;
	}
	
	@Override
	public List<String> getKeywords() {
		return keywords;
	}
	
	@Override
	public String getContact() {
		return contact;
	}

	@Override
	public String getOrganization() {
		return organization;
	}

	@Override
	public String getPosition() {
		return position;
	}

	@Override
	public String getAddressType() {
		return addressType;
	}

	@Override
	public String getAddress() {
		return address;
	}

	@Override
	public String getCity() {
		return city;
	}

	@Override
	public String getState() {
		return state;
	}

	@Override
	public String getZipcode() {
		return zipcode;
	}

	@Override
	public String getCountry() {
		return country;
	}

	@Override
	public String getTelephone() {
		return telephone;
	}

	@Override
	public String getFax() {
		return fax;
	}

	@Override
	public String getEmail() {
		return email;
	}
	
	@Override
	public String getRootId() {
		return root.getId();
	}

	@Override
	public List<LayerRef<?>> getLayers() {
		return root.getLayers();
	}
}
