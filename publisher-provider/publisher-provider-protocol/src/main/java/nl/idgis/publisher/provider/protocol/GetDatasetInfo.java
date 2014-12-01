package nl.idgis.publisher.provider.protocol;

import java.util.Set;

public class GetDatasetInfo extends AbstractDatasetInfo {	
		
	private static final long serialVersionUID = -4776356783003405905L;
	
	private final String id;
	
	public GetDatasetInfo(Set<AttachmentType> attachmentTypes, String id) {
		super(attachmentTypes);
		
		this.id = id;
	}
	
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "GetDatasetInfo [id=" + id + ", attachmentTypes="
				+ attachmentTypes + "]";
	}	
	
}
