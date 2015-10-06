package nl.idgis.publisher.job.manager.messages;

import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;

public class CreateEnsureServiceJob extends CreateServiceJob {

	private static final long serialVersionUID = -8328220486510664915L;
	
	private final String serviceId;
	
	public CreateEnsureServiceJob(String serviceId) {
		this(Optional.empty(), serviceId, false);
	}
	
	public CreateEnsureServiceJob(String serviceId, boolean published) {
		this(Optional.empty(), serviceId, published);
	}
	
	public CreateEnsureServiceJob(Optional<AsyncTransactionRef> transactionRef, String serviceId) {
		this(transactionRef, serviceId, false);
	}
	
	public CreateEnsureServiceJob(Optional<AsyncTransactionRef> transactionRef, String serviceId, boolean published) {
		super(transactionRef, published);
		
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
