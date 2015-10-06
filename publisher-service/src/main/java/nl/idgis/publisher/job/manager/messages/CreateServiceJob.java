package nl.idgis.publisher.job.manager.messages;

import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;

public abstract class CreateServiceJob extends CreateJob {	

	private static final long serialVersionUID = 1622361748773103667L;
	
	protected final boolean published;
	
	protected CreateServiceJob(Optional<AsyncTransactionRef> transactionRef, boolean published) {
		super(transactionRef);
		
		this.published = published;
	}
	
	public boolean isPublished() {
		return published;
	}
}
