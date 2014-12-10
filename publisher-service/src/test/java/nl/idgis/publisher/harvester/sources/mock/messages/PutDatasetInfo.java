package nl.idgis.publisher.harvester.sources.mock.messages;

import java.io.Serializable;

import nl.idgis.publisher.provider.protocol.DatasetInfo;

public class PutDatasetInfo implements Serializable {

	private static final long serialVersionUID = -7222264221576322030L;
	
	private final DatasetInfo datasetInfo;
	
	public PutDatasetInfo(DatasetInfo datasetInfo) {
		this.datasetInfo = datasetInfo;
	}

	public DatasetInfo getDatasetInfo() {
		return datasetInfo;
	}

	@Override
	public String toString() {
		return "PutDatasetInfo [datasetInfo=" + datasetInfo + "]";
	}
	
}
