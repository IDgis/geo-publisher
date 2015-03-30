package nl.idgis.publisher.job.manager.messages;

public abstract class CreateServiceJob extends CreateJob {

	private static final long serialVersionUID = 5160734160584418705L;
	
	protected final boolean published;
	
	protected CreateServiceJob(boolean published) {
		this.published = published;
	}
	
	public boolean isPublished() {
		return published;
	}
}
