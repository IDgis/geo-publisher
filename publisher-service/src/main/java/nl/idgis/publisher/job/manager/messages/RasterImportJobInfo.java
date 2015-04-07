package nl.idgis.publisher.job.manager.messages;

import java.util.List;

import nl.idgis.publisher.domain.job.Notification;

public class RasterImportJobInfo extends ImportJobInfo {

	private static final long serialVersionUID = 5023752628783921132L;

	public RasterImportJobInfo(int id, String categoryId, String dataSourceId, String sourceDatasetId, String datasetId, String datasetName, List<Notification> notifications) {
		super(id, categoryId, dataSourceId, sourceDatasetId, datasetId, datasetName, notifications);
	}

	@Override
	public String toString() {
		return "RasterImportJobInfo [categoryId=" + categoryId
				+ ", dataSourceId=" + dataSourceId + ", sourceDatasetId="
				+ sourceDatasetId + ", datasetId=" + datasetId
				+ ", datasetName=" + datasetName + ", id=" + id + ", jobType="
				+ jobType + ", notifications=" + notifications + "]";
	}
	
}
