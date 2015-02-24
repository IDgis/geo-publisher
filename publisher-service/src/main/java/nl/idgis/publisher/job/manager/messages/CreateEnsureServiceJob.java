package nl.idgis.publisher.job.manager.messages;

public class CreateEnsureServiceJob extends CreateJob {		

	private static final long serialVersionUID = 3566149940479564537L;
	
	private final String serviceId;
	
	public CreateEnsureServiceJob(String serviceId) {
		this.serviceId = serviceId;
	}
	
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "CreateEnsureServiceJob [serviceId=" + serviceId + "]";
	}
}
