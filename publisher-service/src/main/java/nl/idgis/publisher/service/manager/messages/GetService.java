package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

public class GetService implements Serializable {

	private static final long serialVersionUID = -2794875587727238117L;
	
	private final String serviceId;

	public GetService(String serviceId) {
		this.serviceId = serviceId;
	}
	
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "GetService [serviceId=" + serviceId + "]";
	}
}
