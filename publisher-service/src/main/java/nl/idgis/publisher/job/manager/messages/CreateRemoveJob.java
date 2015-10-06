package nl.idgis.publisher.job.manager.messages;

import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;

public class CreateRemoveJob extends CreateJob {	

	private static final long serialVersionUID = 6248635838613711079L;
	
	private final String datasetId;
	
	public CreateRemoveJob(String datasetId) {
		this(Optional.empty(), datasetId);
	}
	
	public CreateRemoveJob(Optional<AsyncTransactionRef> transactionRef, String datasetId) {
		super(transactionRef);
		
		this.datasetId = datasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}
	
	@Override
	public String toString() {
		return "CreateRemoveJob [datasetId=" + datasetId + "]";
	}
}
