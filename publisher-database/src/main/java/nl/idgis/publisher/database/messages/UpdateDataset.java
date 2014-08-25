package nl.idgis.publisher.database.messages;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class UpdateDataset extends CreateUpdateDataset {

	public UpdateDataset(String datasetIdentification, String datasetName,
			String sourceDatasetIdentification, List<Column> columnList,
			final String filterConditions) {
		super(datasetIdentification, datasetName, sourceDatasetIdentification,
				columnList, filterConditions);
	}

	private static final long serialVersionUID = -1881219882327450211L;


}
