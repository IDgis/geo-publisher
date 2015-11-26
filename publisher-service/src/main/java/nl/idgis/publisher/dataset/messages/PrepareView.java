package nl.idgis.publisher.dataset.messages;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

public class PrepareView implements Serializable, AsyncTransactional {

	private static final long serialVersionUID = 2907896996903582989L;

	private final AsyncTransactionRef transactionRef;

	private final String datasetId;
	
	public PrepareView(String datasetId) {
		this(Optional.empty(), datasetId);
	}
	
	public PrepareView(Optional<AsyncTransactionRef> transactionRef, String datasetId) {
		this.transactionRef = transactionRef.orElse(null);
		this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
	}

	@Override
	public Optional<AsyncTransactionRef> getTransactionRef() {
		return Optional.ofNullable(transactionRef);
	}
	
	public String getDatasetId() {
		return datasetId;
	}

}
