package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;

public class GetGroupLayer implements Serializable {
	
	private static final long serialVersionUID = 1741129277383747405L;

	private final AsyncTransactionRef transactionRef;
	
	private final String groupLayerId;
	
	public GetGroupLayer(String groupLayerId) {
		this(null, groupLayerId);
	}

	public GetGroupLayer(AsyncTransactionRef transactionRef, String groupLayerId) {
		this.transactionRef = transactionRef;
		this.groupLayerId = groupLayerId;
	}
	
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
