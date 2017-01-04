package nl.idgis.publisher.provider.messages;

import java.io.Serializable;

public class SkipDataset implements Serializable {

	private static final long serialVersionUID = -3709472005223070750L;
	
	private final String identification;
	
	public SkipDataset(String identification) {
		this.identification = identification;
	}
	
	public String getIdentification() {
		return identification;
	}

	@Override
	public String toString() {
		return "SkipDataset [identification=" + identification + "]";
	}
}
