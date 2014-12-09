package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

public class DatasetNotFound implements Serializable {

	private static final long serialVersionUID = 6029144875076378782L;
	
	private final String identification;
	
	public DatasetNotFound(String identification) {
		this.identification = identification;
	}
	
	public String getIdentification() {
		return identification;
	}

	@Override
	public String toString() {
		return "DatasetNotFound [identification=" + identification + "]";
	}
}
