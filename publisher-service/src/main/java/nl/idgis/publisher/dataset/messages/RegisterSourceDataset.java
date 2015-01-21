package nl.idgis.publisher.dataset.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.service.Dataset;

public class RegisterSourceDataset implements Serializable {

	private static final long serialVersionUID = 948150634994468088L;

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
