package nl.idgis.publisher.job.manager.messages;

public class CreateEnsureServiceJob extends CreateServiceJob {

	private static final long serialVersionUID = 7006360116324771656L;
	
	private final String serviceId;
	
	public CreateEnsureServiceJob(String serviceId) {
		this(serviceId, false);
	}
	
	public CreateEnsureServiceJob(String serviceId, boolean published) {
		super(published);
		
		this.serviceId = serviceId;
	}
	
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "CreateEnsureServiceJob [serviceId=" + serviceId
				+ ", published=" + published + "]";
	}
	
}
