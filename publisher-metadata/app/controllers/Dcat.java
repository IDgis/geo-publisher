package controllers;
import java.util.List;

public class Dcat {

	private final String typeDcatDataset =	"dcat:Dataset";
	private String identifier;
	private String title;
	private String description;
	private String issued;
	private String modified;
	private String publisherName;
	private String accessLevel;	
	private String landingPage;
	private String license;
	private String spatial;
	// private String webService;
	
	private String[] keyword;	
	private String[] theme;	
	
	private final String typeDcatContact =	"vcard:Contact";
	private String contactPointFn;
	private String contactPointHasEmail;
	
		/*
	 distribution	[6]
	 */
		
	public Dcat(String accessLevel, String identifier, String title, String publisherName) {
		this.identifier = identifier;
		this.title = title;
		// description
		// issued;
		// modified;
		this.publisherName = publisherName;
		this.accessLevel = accessLevel;
		//landingPage
		//license
		//spatial
		
		//keyword
		//theme
		
		//contactPointFn
		//contactPointHasEmail
		}
	
	
		
	public Dcat(String description, String[] theme, String landingPage, String publisherName, String issued,
				String modified, String contactPointFn, String identifier, String title, String accessLevel,
				String contactPointHasEmail, String spatial, String license, String[] keyword) {
			this.description = description;
			this.theme = theme;
			this.landingPage = landingPage;
			this.publisherName = publisherName;
			this.issued = issued;
			
			this.modified = modified;
			this.contactPointFn = contactPointFn;
			this.identifier = identifier;
			this.title = title;
			this.accessLevel = accessLevel;
			
			this.contactPointHasEmail = contactPointHasEmail;
			this.spatial = spatial;
			this.license = license;
			this.keyword = keyword;
		}



	public String getAccessLevel() {
		return accessLevel;
	}
	
	public String getTitle() {
		return title;
	}

	public String getIdentifier() {
		return identifier;
	}

	public String getTypeDcatDataset() {
		return typeDcatDataset;
	}

	public String getDescription() {
		return description;
	}

	public String getIssued() {
		return issued;
	}

	public String getModified() {
		return modified;
	}

	public String getPublisherName() {
		return publisherName;
	}

	public String getLandingPage() {
		return landingPage;
	}

	public String getLicense() {
		return license;
	}

	public String getSpatial() {
		return spatial;
	}

	public String[] getKeyword() {
		return keyword;
	}

	public String[] getTheme() {
		return theme;
	}

	public String getTypeDcatContact() {
		return typeDcatContact;
	}

	public String getContactPointFn() {
		return contactPointFn;
	}

	public String getContactPointHasEmail() {
		return contactPointHasEmail;
	}
}
