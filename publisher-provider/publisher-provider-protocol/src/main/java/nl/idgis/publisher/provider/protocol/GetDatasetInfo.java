package nl.idgis.publisher.provider.protocol;

import java.util.Set;

public class GetDatasetInfo extends AbstractDatasetInfo {	
		
	private static final long serialVersionUID = -4776356783003405905L;
	
	private final String identification;
	
	public GetDatasetInfo(Set<AttachmentType> attachmentTypes, String identification) {
		super(attachmentTypes);
		
		this.identification = identification;
	}
	
	public String getIdentification() {
		return identification;
	}

	@Override
	public String toString() {
		return "GetDatasetInfo [identification=" + identification + ", attachmentTypes="
				+ attachmentTypes + "]";
	}	
	
}
