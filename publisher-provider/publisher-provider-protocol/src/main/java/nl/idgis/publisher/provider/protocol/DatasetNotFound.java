package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

/**
 * Requested dataset doesn't exists.
 * 
 * @author copierrj
 *
 */
public class DatasetNotFound implements Serializable {

	private static final long serialVersionUID = 6029144875076378782L;
	
	private final String identification;
	
	/**
	 * Creates a dataset not found message.
	 * @param identification specified in request.
	 */
	public DatasetNotFound(String identification) {
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
		return "DatasetNotFound [identification=" + identification + "]";
	}
}
