package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

/**
 * Base class for dataset requests.
 * 
 * @author copierrj
 *
 */
public abstract class AbstractGetDatasetRequest implements Serializable {
		
	private static final long serialVersionUID = 1586515676745659189L;
	
	private final String identification;
	
	/**
	 * 
	 * @param identification specifies which dataset to retrieve.
	 */
	public AbstractGetDatasetRequest(String identification) {
		this.identification = identification;
	}
	
	/**
	 * 
	 * @return identification of requested dataset
	 */
	public String getIdentification() {
		return this.identification;
	}
}
