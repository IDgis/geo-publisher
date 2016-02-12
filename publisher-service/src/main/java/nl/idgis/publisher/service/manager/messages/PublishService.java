package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

public class PublishService implements Serializable, AsyncTransactional {
	
	private static final long serialVersionUID = 7938304214996997626L;

	private final AsyncTransactionRef transactionRef;
	
	private final String serviceId;
	
	private final String environmentId;
	
	public PublishService(String serviceId) {
		this(serviceId, Optional.empty());
	}
	
	public PublishService(String serviceId, Optional<String> environmentId) {
		this(Optional.empty(), serviceId, environmentId);
	}

	public PublishService(Optional<AsyncTransactionRef> transactionRef, String serviceId, Optional<String> environmentId) {
		this.transactionRef = transactionRef.orElse(null);
		this.serviceId = serviceId;
		this.environmentId = environmentId.orElse(null);
	}
	
	@Override
	public Optional<AsyncTransactionRef> getTransactionRef() {
		return Optional.ofNullable(transactionRef);
	}

	public String getServiceId() {
		return serviceId;
	}

	public Optional<String> getEnvironmentId() {
		return Optional.ofNullable(environmentId);
	}

	@Override
	public String toString() {
		return "PublishService [transactionRef=" + transactionRef
				+ ", serviceId=" + serviceId + ", environmentId="
				+ environmentId + "]";
	}
}
