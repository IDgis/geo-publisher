package nl.idgis.publisher.harvester.sources.mock;

import java.io.Serializable;
import java.util.Set;

import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.Record;

public class Dataset implements Serializable {

	private static final long serialVersionUID = 6884220290002048260L;

	private final DatasetInfo datasetInfo;
	
	private final Set<Record> records;
	
	public Dataset(DatasetInfo datasetInfo, Set<Record> records) {
		this.datasetInfo = datasetInfo;
		this.records = records;
	}

	public DatasetInfo getDatasetInfo() {
		return datasetInfo;
	}

	public Set<Record> getRecords() {
		return records;
	}
}
