package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

public class GetStyles implements AsyncTransactional, Serializable {

	private static final long serialVersionUID = -4395497329460362997L;

	private final AsyncTransactionRef transactionRef;
	
	private final String serviceId;
	
	public GetStyles(String serviceId) {
		this(null, serviceId);
	}
	
	public GetStyles(AsyncTransactionRef transactionRef, String serviceId) {
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
		return "GetStyles [transactionRef=" + transactionRef + ", serviceId="
				+ serviceId + "]";
	}	
}
