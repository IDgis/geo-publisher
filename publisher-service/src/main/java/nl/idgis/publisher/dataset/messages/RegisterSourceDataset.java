package nl.idgis.publisher.dataset.messages;

import java.io.Serializable;

import nl.idgis.publisher.domain.service.VectorDataset;

public class RegisterSourceDataset implements Serializable {

	private static final long serialVersionUID = 5407332380484864459L;
	
	private final String dataSource;
	
	private final VectorDataset dataset;
	
	public RegisterSourceDataset(String dataSource, VectorDataset dataset) {
		this.dataSource = dataSource;
		this.dataset = dataset;
	}

	public String getDataSource() {
		return dataSource;
	}

	public VectorDataset getDataset() {
		return dataset;
	}

	@Override
	public String toString() {
		return "RegisterSourceDataset [dataSource=" + dataSource + ", dataset="
				+ dataset + "]";
	}
	
}
