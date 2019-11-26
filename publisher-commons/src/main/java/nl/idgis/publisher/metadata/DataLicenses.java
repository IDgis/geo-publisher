package nl.idgis.publisher.metadata;

public enum DataLicenses {
	mark ("Geen beperkingen"),
	zero ("Geen beperkingen"),
	by ("Naamsvermelding verplicht, organisatienaam"),
	bySa ("Gelijk Delen, Naamsvermelding verplicht, organisatienaam"),
	byNc ("Niet Commercieel, Naamsvermelding verplicht, organisatienaam"),
	byNcSa ("Niet Commercieel, Gelijk Delen, Naamsvermelding verplicht, organisatienaam"),
	byNd ("Geen Afgeleide Werken, Naamsvermelding verplicht, organisatienaam"),
	byNcNd ("Niet Commercieel, Geen Afgeleide Werken, Naamsvermelding verplicht, organisatienaam");
	
	private final String description;

	DataLicenses(String description) {
		this.description = description;
	}

	public String description() {
		return description;
	}
}
