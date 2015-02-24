package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.List;

public class ServiceIndex implements Serializable {

	private static final long serialVersionUID = 9132335266500152082L;

	private final List<String> serviceNames;
	
	private final List<String> styleNames;
	
	public ServiceIndex(List<String> serviceNames, List<String> styleNames) {
		this.serviceNames = serviceNames;
		this.styleNames = styleNames;
	}

	public List<String> getServiceNames() {
		return serviceNames;
	}

	public List<String> getStyleNames() {
		return styleNames;
	}

	@Override
	public String toString() {
		return "ServiceIndex [serviceNames=" + serviceNames + ", styleNames="
				+ styleNames + "]";
	}
}
