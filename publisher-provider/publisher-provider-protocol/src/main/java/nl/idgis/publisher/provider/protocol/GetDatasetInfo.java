package nl.idgis.publisher.provider.protocol;

import java.util.Set;

/**
 * Request a {@link DatasetInfo} for a specific dataset.
 * @author copierrj
 *
 */
public class GetDatasetInfo extends AbstractDatasetInfoRequest {	
		
	private static final long serialVersionUID = -4776356783003405905L;
	
	private final String identification;
	
	/**
	 * Creates a get dataset info request.
	 * @param attachmentTypes specifies which attachment types to fetch.
	 * @param identification the identifier of the dataset.
	 */
	public GetDatasetInfo(Set<AttachmentType> attachmentTypes, String identification) {
		super(attachmentTypes);
		
		this.identification = identification;
	}
	
	/**
	 * 
	 * @return the dataset identifier
	 */
	public String getIdentification() {
		return identification;
	}

	@Override
	public String toString() {
		return "GetDatasetInfo [identification=" + identification + ", attachmentTypes="
				+ attachmentTypes + "]";
	}	
	
}
