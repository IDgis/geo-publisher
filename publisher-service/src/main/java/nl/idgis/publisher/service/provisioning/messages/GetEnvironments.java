package nl.idgis.publisher.service.provisioning.messages;

import java.io.Serializable;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

public class GetEnvironments implements Serializable, AsyncTransactional {

	private static final long serialVersionUID = -6272595108178262037L;
	
	private final AsyncTransactionRef transactionRef;
	
	private final String serviceId;
	
	public GetEnvironments(String serviceId) {
		this(Optional.empty(), serviceId);
	}
	
	public GetEnvironments(Optional<AsyncTransactionRef> transactionRef, String serviceId) {
		this.transactionRef = transactionRef.orElse(null);
		this.serviceId = serviceId;
	}
	
	@Override
	public Optional<AsyncTransactionRef> getTransactionRef() {
		return Optional.ofNullable(transactionRef);
	}

	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String toString() {
		return "GetEnvironments [transactionRef=" + transactionRef
				+ ", serviceId=" + serviceId + "]";
	}
}
