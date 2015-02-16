package nl.idgis.publisher.service.geoserver.rest;

public class ServiceSettings {
	
	private final ServiceType serviceType;

	private final String title;
	
	public ServiceSettings(ServiceType serviceType, String title) {
		this.serviceType = serviceType;
		this.title = title;
	}

	public ServiceType getServiceType() {
		return serviceType;
	}

	public String getTitle() {
		return title;
	}
}
