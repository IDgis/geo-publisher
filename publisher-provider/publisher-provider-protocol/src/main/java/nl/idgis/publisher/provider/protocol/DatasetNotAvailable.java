package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

/**
 * Requested dataset is not available. However, it does exists.
 * 
 * @author copierrj
 *
 */
public class DatasetNotAvailable implements Serializable {
	
	private static final long serialVersionUID = -1361550769437152641L;
	
	private final String identification;
	
	/**
	 * Creates a dataset not available message.
	 * @param identification specified in request.
	 */
	public DatasetNotAvailable(String identification) {
		this.identification = identification;
	}
	
	/**
	 * 
	 * @return identification specified in request
	 */
	public String getIdentification() {
		return identification;
	}

	@Override
	public String toString() {
		return "DatasetNotAvailable [identification=" + identification + "]";
	}
}
