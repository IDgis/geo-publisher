package nl.idgis.publisher.dataset.messages;

import java.io.Serializable;

public class HarvestSessionDatasetDuplicate implements Serializable {
	
	private static final long serialVersionUID = 7678467928756500068L;

	private final String dataSourceId;
	
	private final String datasetId;
	
	public HarvestSessionDatasetDuplicate(String dataSourceId, String datasetId) {
		this.dataSourceId = dataSourceId;
		this.datasetId = datasetId;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	@Override
	public String toString() {
		return "HarvestSessionDatasetDuplicate [dataSourceId=" + dataSourceId + ", datasetId="
				+ datasetId + "]";
	}
}
