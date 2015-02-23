package nl.idgis.publisher.job.manager.messages;

public class EnsureServiceJobInfo extends ServiceJobInfo {	

	private static final long serialVersionUID = -5565063840815339459L;
	
	private final String serviceId;	
	
	public EnsureServiceJobInfo(int id, String serviceId) {
		super(id);
		
		this.serviceId = serviceId;
	}

	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "EnsureServiceJobInfo [serviceId=" + serviceId + "]";
	}
}
