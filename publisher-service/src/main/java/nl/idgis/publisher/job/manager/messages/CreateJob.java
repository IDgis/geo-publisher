package nl.idgis.publisher.job.manager.messages;

import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;
import nl.idgis.publisher.database.AsyncTransactional;

public abstract class CreateJob extends JobManagerRequest implements AsyncTransactional {	

	private static final long serialVersionUID = 8831792530299847484L;
	
	private final AsyncTransactionRef transactionRef;

	protected CreateJob(Optional<AsyncTransactionRef> transactionRef) {
		this.transactionRef = transactionRef.orElse(null);
	}
	
	public Optional<AsyncTransactionRef> getTransactionRef() {
		return Optional.ofNullable(transactionRef);
	}
}
