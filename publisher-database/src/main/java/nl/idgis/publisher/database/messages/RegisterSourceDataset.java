package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.service.Dataset;

public class RegisterSourceDataset extends Query {
	
	private static final long serialVersionUID = 1L;
	
	private final String dataSource;
	private final Dataset dataset;
	
	public RegisterSourceDataset(String dataSource, Dataset dataset) {
		this.dataSource = dataSource;
		this.dataset = dataset;
	}

	public String getDataSource() {
		return dataSource;
	}

	public Dataset getDataset() {
		return dataset;
	}

	@Override
	public String toString() {
		return "RegisterSourceDataset [dataSource=" + dataSource + ", dataset="
				+ dataset + "]";
	}
	
}
