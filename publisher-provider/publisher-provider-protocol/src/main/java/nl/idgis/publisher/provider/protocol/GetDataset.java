package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

public abstract class GetDataset implements Serializable {

	private static final long serialVersionUID = -8635139807073207401L;
	
	private final String identification;
	
	public GetDataset(String identification) {
		this.identification = identification;
	}
	
	public String getIdentification() {
		return this.identification;
	}
}
