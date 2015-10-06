package nl.idgis.publisher.job.manager.messages;

import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;

public class CreateImportJob extends CreateJob {

	private static final long serialVersionUID = -8706899200369431138L;
	
	private final String datasetId;
	
	public CreateImportJob(String datasetId) {
		this(Optional.empty(), datasetId);
	}
	
	public CreateImportJob(Optional<AsyncTransactionRef> transactionRef, String datasetId) {
		super(transactionRef);
		
		this.datasetId = datasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "CreateImportJob [datasetId=" + datasetId + "]";
	}
}
