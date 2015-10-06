package nl.idgis.publisher.job.manager.messages;

import java.util.Optional;

import nl.idgis.publisher.database.AsyncTransactionRef;

public class CreateVacuumServiceJob extends CreateServiceJob {

	private static final long serialVersionUID = 6223903107754891070L;

	public CreateVacuumServiceJob(Optional<AsyncTransactionRef> transactionRef) {
		this(transactionRef, false);
	}
	
	public CreateVacuumServiceJob(Optional<AsyncTransactionRef> transactionRef, boolean published) {
		super(transactionRef, published);
	}
	
	public CreateVacuumServiceJob() {
		this(Optional.empty(), false);
	}
	
	public CreateVacuumServiceJob(boolean published) {
		super(Optional.empty(), published);
	}

	@Override
	public String toString() {
		return "CreateVacuumServiceJob [published=" + published + "]";
	}
	
}
