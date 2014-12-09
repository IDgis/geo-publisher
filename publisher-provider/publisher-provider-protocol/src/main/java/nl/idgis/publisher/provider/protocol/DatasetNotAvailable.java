package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

public class DatasetNotAvailable implements Serializable {
	
	private static final long serialVersionUID = -1361550769437152641L;
	
	private final String identification;
	
	public DatasetNotAvailable(String identification) {
		this.identification = identification;
	}
	
	public String getIdentification() {
		return identification;
	}

	@Override
	public String toString() {
		return "DatasetNotAvailable [identification=" + identification + "]";
	}
}
