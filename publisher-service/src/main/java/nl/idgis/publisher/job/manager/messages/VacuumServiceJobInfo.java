package nl.idgis.publisher.job.manager.messages;

public class VacuumServiceJobInfo extends ServiceJobInfo {	

	private static final long serialVersionUID = -8653794090115864277L;
	
	public VacuumServiceJobInfo(int id) {
		this(id, false);
	}

	public VacuumServiceJobInfo(int id, boolean published) {
		super(id, published);
	}

	@Override
	public String toString() {
		return "VacuumServiceJobInfo [published=" + published + "]";
	}

}
