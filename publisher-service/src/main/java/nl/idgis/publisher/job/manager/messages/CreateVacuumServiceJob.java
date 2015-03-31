package nl.idgis.publisher.job.manager.messages;

public class CreateVacuumServiceJob extends CreateServiceJob {

	private static final long serialVersionUID = 1228775541762439568L;

	public CreateVacuumServiceJob() {
		this(false);
	}
	
	public CreateVacuumServiceJob(boolean published) {
		super(published);
	}

	@Override
	public String toString() {
		return "CreateVacuumServiceJob [published=" + published + "]";
	}
	
}
