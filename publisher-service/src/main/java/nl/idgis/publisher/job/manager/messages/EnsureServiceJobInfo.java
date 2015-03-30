package nl.idgis.publisher.job.manager.messages;

public class EnsureServiceJobInfo extends ServiceJobInfo {	

	private static final long serialVersionUID = 1836336574151204818L;
	
	private final String serviceId;	
	
	public EnsureServiceJobInfo(int id, String serviceId) {
		this(id, serviceId, false);
	}
	
	public EnsureServiceJobInfo(int id, String serviceId, boolean published) {
		super(id, published);
		
		this.serviceId = serviceId;
	}

	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "EnsureServiceJobInfo [serviceId=" + serviceId + ", published="
				+ published + "]";
	}
	
}
