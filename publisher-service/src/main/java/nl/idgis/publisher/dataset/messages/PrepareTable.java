package nl.idgis.publisher.dataset.messages;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

import nl.idgis.publisher.domain.service.Column;

public class PrepareTable implements Serializable, AsyncTransactional {	
	
	private static final long serialVersionUID = -8913527096636886307L;

	private final AsyncTransactionRef transactionRef;

	private final String datasetId;
	
	private final List<Column> columns;
	
	public PrepareTable(String datasetId, List<Column> columns) {
		this(Optional.empty(), datasetId, columns);
	}
	
	public PrepareTable(Optional<AsyncTransactionRef> transactionRef, String datasetId, List<Column> columns) {
		this.transactionRef = transactionRef.orElse(null);
		this.datasetId = Objects.requireNonNull(datasetId, "datasetId should not be null");
		this.columns = Objects.requireNonNull(columns, "columns should not be null");
	}

	@Override
	public Optional<AsyncTransactionRef> getTransactionRef() {
		return Optional.ofNullable(transactionRef);
	}
	
	public String getDatasetId() {
		return datasetId;
	}
	
	public List<Column> getColumns() {
		return columns;
	}
}
