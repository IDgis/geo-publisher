package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

public class GetGroupLayer implements Serializable, AsyncTransactional {
	
	private static final long serialVersionUID = -8813198397459281097L;

	private final AsyncTransactionRef transactionRef;
	
	private final String groupLayerId;
	
	public GetGroupLayer(String groupLayerId) {
		this(null, groupLayerId);
	}

	public GetGroupLayer(AsyncTransactionRef transactionRef, String groupLayerId) {
		this.transactionRef = transactionRef;
		this.groupLayerId = groupLayerId;
	}
	
	@Override
	public Optional<AsyncTransactionRef> getTransactionRef() {
		return Optional.ofNullable(transactionRef);
	}
	
	public String getGroupLayerId() {
		return groupLayerId;
	}

	@Override
	public String toString() {
		return "GetGroupLayer [transactionRef=" + transactionRef
				+ ", groupLayerId=" + groupLayerId + "]";
	}
	
}
