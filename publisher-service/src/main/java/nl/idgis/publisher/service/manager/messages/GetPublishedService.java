package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

public class GetPublishedService implements Serializable, AsyncTransactional {
	
	private static final long serialVersionUID = 1810764496059668719L;
	
	private final AsyncTransactionRef transactionRef;
	
	private final String serviceId;
	
	public GetPublishedService(String serviceId) {
		this(Optional.empty(), serviceId);
	}
	
	public GetPublishedService(Optional<AsyncTransactionRef> transactionRef, String serviceId) {
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
		return "GetPublishedService [transactionRef=" + transactionRef
				+ ", serviceId=" + serviceId + "]";
	}

}
