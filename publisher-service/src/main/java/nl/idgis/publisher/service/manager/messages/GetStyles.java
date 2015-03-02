package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

public class GetStyles implements Serializable {

	private static final long serialVersionUID = 5244312292242616612L;
	
	private final String serviceId;
	
	public GetStyles(String serviceId) {
		this.serviceId = serviceId;
	}
	
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "GetStyles [serviceId=" + serviceId + "]";
	}
}
