package nl.idgis.publisher.database.messages;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class UpdateDataset extends CreateDataset {

	private static final long serialVersionUID = -1881219882327450211L;

	public UpdateDataset(String datasetIdentification, String sourceDatasetIdentification,
			List<Column> columnList) {
		super(datasetIdentification, sourceDatasetIdentification, columnList);
	}

}
