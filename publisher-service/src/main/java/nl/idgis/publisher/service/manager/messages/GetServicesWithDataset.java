package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

public class GetServicesWithDataset implements Serializable {

	private static final long serialVersionUID = -935935604285802189L;
	
	private final String datasetId;
	
	public GetServicesWithDataset (final String datasetId) {
		this.datasetId = datasetId;
	}
	
	public String getDatasetId () {
		return datasetId;
	}
	
	@Override
	public String toString () {
		return "GetServicesWithDataset [datasetId=" + datasetId + "]";
	}
}
