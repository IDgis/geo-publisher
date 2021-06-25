package nl.idgis.publisher.dataset.messages;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

import nl.idgis.publisher.domain.service.Column;

public class PrepareTable implements Serializable, AsyncTransactional {
	
	private static final long serialVersionUID = -3082360967366368498L;

	private final AsyncTransactionRef transactionRef;
	
	private final String tmpTable;

	private final String datasetId;
	
	private final List<Column> columns;
	
	private final long insertCount;
	
	public PrepareTable(String tmpTable, String datasetId, List<Column> columns, long insertCount) {
		this(Optional.empty(), tmpTable, datasetId, columns, insertCount);
	}
	
	public PrepareTable(Optional<AsyncTransactionRef> transactionRef, String tmpTable, String datasetId, List<Column> columns, long insertCount) {
		this.transactionRef = transactionRef.orElse(null);
		this.tmpTable = Objects.requireNonNull(tmpTable, "tmpTable should not be null");
		this.datasetId = Objects.requireNonNull(datasetId, "datasetId should not be null");
		this.columns = Objects.requireNonNull(columns, "columns should not be null");
		this.insertCount = insertCount;
	}

	@Override
	public Optional<AsyncTransactionRef> getTransactionRef() {
		return Optional.ofNullable(transactionRef);
	}
	
	public String getTmpTable() {
		return tmpTable;
	}
	
	public String getDatasetId() {
		return datasetId;
	}
	
	public List<Column> getColumns() {
		return columns;
	}
	
	public long getInsertCount() {
		return insertCount;
	}
}
