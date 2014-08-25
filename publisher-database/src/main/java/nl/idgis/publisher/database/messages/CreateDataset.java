package nl.idgis.publisher.database.messages;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class CreateDataset extends CreateUpdateDataset {

	public CreateDataset(String datasetIdentification, String datasetName,
			String sourceDatasetIdentification, List<Column> columnList, 
			final String filterConditions) {
		super(datasetIdentification, datasetName, sourceDatasetIdentification,
				columnList, filterConditions);
	}

	private static final long serialVersionUID = -7841672424518750710L;

}
