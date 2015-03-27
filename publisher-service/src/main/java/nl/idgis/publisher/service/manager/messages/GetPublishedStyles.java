package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

public class GetPublishedStyles implements AsyncTransactional, Serializable {

	private static final long serialVersionUID = 2248587462480702295L;

	private final AsyncTransactionRef transactionRef;
	
	private final String serviceId;
	
	public GetPublishedStyles(Optional<AsyncTransactionRef> transactionRef, String serviceId) {
		this.transactionRef = transactionRef.orElse(null);
		this.serviceId = serviceId;
	}

	@Override
	public Optional<AsyncTransactionRef> getTransactionRef() {
		return Optional.ofNullable(transactionRef);
	}
	
	public String getServiceId() {
		return this.serviceId;
	}

	@Override
	public String toString() {
		return "GetPublishedStyles [transactionRef=" + transactionRef
				+ ", serviceId=" + serviceId + "]";
	}
}
