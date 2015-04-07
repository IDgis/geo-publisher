package nl.idgis.publisher.job.manager.messages;

import java.util.List;

import nl.idgis.publisher.domain.job.Notification;

public class RasterImportJobInfo extends ImportJobInfo {

	private static final long serialVersionUID = -1750621185529038983L;

	public RasterImportJobInfo(int id, String categoryId, String dataSourceId, String sourceDatasetId, String externalSourceDatasetId, String datasetId, String datasetName, List<Notification> notifications) {
		super(id, categoryId, dataSourceId, sourceDatasetId, externalSourceDatasetId, datasetId, datasetName, notifications);
	}

	@Override
	public String toString() {
		return "RasterImportJobInfo [categoryId=" + categoryId
				+ ", dataSourceId=" + dataSourceId + ", sourceDatasetId="
				+ sourceDatasetId + ", externalSourceDatasetId="
				+ externalSourceDatasetId + ", datasetId=" + datasetId
				+ ", datasetName=" + datasetName + ", id=" + id + ", jobType="
				+ jobType + ", notifications=" + notifications + "]";
	}	
	
}
