package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

public class PublishService implements Serializable, AsyncTransactional {

	private static final long serialVersionUID = -8391524952091189779L;
	
	private final AsyncTransactionRef transactionRef;
	
	private final String serviceId;
	
	private final Set<String> environmentIds;
	
	public PublishService(String serviceId, Set<String> environmentIds) {
		this(Optional.empty(), serviceId, environmentIds);
	}

	public PublishService(Optional<AsyncTransactionRef> transactionRef, String serviceId, Set<String> environmentIds) {
		this.transactionRef = transactionRef.orElse(null);
		this.serviceId = serviceId;
		this.environmentIds = environmentIds;
	}
	
	@Override
	public Optional<AsyncTransactionRef> getTransactionRef() {
		return Optional.ofNullable(transactionRef);
	}

	public String getServiceId() {
		return serviceId;
	}

	public Set<String> getEnvironmentIds() {
		return environmentIds;
	}

	@Override
	public String toString() {
		return "PublishService [transactionRef=" + transactionRef
				+ ", serviceId=" + serviceId + ", environmentIds="
				+ environmentIds + "]";
	}
}
