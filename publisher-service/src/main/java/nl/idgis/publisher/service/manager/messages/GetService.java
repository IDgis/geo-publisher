package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

public class GetService implements Serializable, AsyncTransactional {	
	
	private static final long serialVersionUID = 7541065168553854852L;

	private final AsyncTransactionRef transactionRef;
	
	private final String serviceId;
	
	public GetService(String serviceId) {
		this(null, serviceId);
	}

	public GetService(AsyncTransactionRef transactionRef, String serviceId) {
		this.transactionRef = transactionRef;
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
		return "GetService [transactionRef=" + transactionRef + ", serviceId="
				+ serviceId + "]";
	}
	
}
