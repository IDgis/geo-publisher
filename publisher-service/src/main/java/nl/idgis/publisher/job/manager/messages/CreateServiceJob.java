package nl.idgis.publisher.job.manager.messages;

public class CreateServiceJob extends CreateJob {
	
	private static final long serialVersionUID = -1965301047800943566L;
	
	private final String serviceId;
	
	public CreateServiceJob(String serviceId) {
		this.serviceId = serviceId;
	}
	
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "CreateServiceJob [serviceId=" + serviceId + "]";
	}
}
