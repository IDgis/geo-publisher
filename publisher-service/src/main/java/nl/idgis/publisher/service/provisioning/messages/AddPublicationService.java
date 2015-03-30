package nl.idgis.publisher.service.provisioning.messages;

import nl.idgis.publisher.service.provisioning.ServiceInfo;

public class AddPublicationService extends UpdateServiceInfo {

	private static final long serialVersionUID = -5728424049640543301L;
	
	private final String environmentId;
	
	private final ServiceInfo serviceInfo;

	public AddPublicationService(String environmentId, ServiceInfo serviceInfo) {
		this.environmentId = environmentId;
		this.serviceInfo = serviceInfo;
	}
	
	public String getEnvironmentId() {
		return environmentId;
	}

	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	@Override
	public String toString() {
		return "AddPublicationService [environmentId=" + environmentId
				+ ", serviceInfo=" + serviceInfo + "]";
	}
	
}
