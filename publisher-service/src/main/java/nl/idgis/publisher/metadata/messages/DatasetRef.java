package nl.idgis.publisher.metadata.messages;

import java.util.Objects;
import java.util.Set;

public class DatasetRef {
	
	private final String datasetId;

	private final String uuid;
	
	private final String fileUuid;
	
	private final Set<String> layerNames;
	
	public DatasetRef(String datasetId, String uuid, String fileUuid, Set<String> layerNames) {
		this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
		this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
		this.fileUuid = Objects.requireNonNull(fileUuid, "fileUuid must not be null");
		this.layerNames = Objects.requireNonNull(layerNames, "layerNames must not be null");
	}
	
	public String getDatasetId() {
		return datasetId;
	}

	public String getUuid() {
		return uuid;
	}

	public String getFileUuid() {
		return fileUuid;
	}
	
	public Set<String> getLayerNames() {
		return layerNames;
	}
}
