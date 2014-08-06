package nl.idgis.publisher.database.messages;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class UpdateDataset extends CreateDataset {

	public UpdateDataset(String datasetIdentification, String datasetName,
			String sourceDatasetIdentification, List<Column> columnList) {
		super(datasetIdentification, datasetName, sourceDatasetIdentification,
				columnList);
	}

	private static final long serialVersionUID = -1881219882327450211L;


}
