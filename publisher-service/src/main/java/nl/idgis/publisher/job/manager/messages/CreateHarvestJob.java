package nl.idgis.publisher.job.manager.messages;

import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;

public class CreateHarvestJob extends CreateJob {	

	private static final long serialVersionUID = 3777253669226117890L;
	
	private final String dataSourceId;
	
	public CreateHarvestJob(String dataSourceId) {
		this(Optional.empty(), dataSourceId);
	}
	
	public CreateHarvestJob(Optional<AsyncTransactionRef> transactionRef, String dataSourceId) {
		super(transactionRef);
		
		this.dataSourceId = dataSourceId;
	}
	
	public String getDataSourceId() {
		return dataSourceId;
	}

	@Override
	public String toString() {
		return "CreateHarvestJob [dataSourceId=" + dataSourceId + "]";
	}
}
