package nl.idgis.publisher.provider.protocol;

/**
 * Request a vector dataset.
 *  
 * @author copierrj
 *
 */
public class GetRasterDataset extends AbstractGetDatasetRequest {

	private static final long serialVersionUID = 6422979234965715075L;

	/**
	 * Creates a get raster dataset request.
	 * @param identification the identifier of the dataset. 
	 */
	public GetRasterDataset(String identification) {
		super(identification);
	}

	@Override
	public String toString() {
		return "GetRasterDataset [identification=" + getIdentification()
				+ "]";
	}	
}
