package nl.idgis.publisher.metadata.messages;

public class KeepServiceMetadata extends KeepMetadata {	

	private static final long serialVersionUID = -6062420478081152215L;
	
	private final String serviceId;	
	
	public KeepServiceMetadata(String serviceId) {
		this.serviceId = serviceId;
	}
	
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "KeepServiceMetadata [serviceId=" + serviceId + "]";
	}
}
