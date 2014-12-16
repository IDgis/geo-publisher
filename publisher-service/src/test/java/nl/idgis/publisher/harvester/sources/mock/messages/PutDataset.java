package nl.idgis.publisher.harvester.sources.mock.messages;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.Record;

public class PutDataset implements Serializable {

	private static final long serialVersionUID = -7222264221576322030L;
	
	private final DatasetInfo datasetInfo;
	
	private final Set<Record> records;
	
	public PutDataset(DatasetInfo datasetInfo) {
		this(datasetInfo, Collections.<Record>emptySet());
	}
	
	public PutDataset(DatasetInfo datasetInfo, Set<Record> records) {
		this.datasetInfo = datasetInfo;
		this.records = records;
	}

	public DatasetInfo getDatasetInfo() {
		return datasetInfo;
	}
	
	public Set<Record> getRecords() {
		return records;
	}
	
	@Override
	public String toString() {
		return "PutDataset [datasetInfo=" + datasetInfo + ", records="
				+ records + "]";
	}	
	
}
