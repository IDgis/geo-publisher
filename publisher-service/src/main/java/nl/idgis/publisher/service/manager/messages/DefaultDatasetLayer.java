package nl.idgis.publisher.service.manager.messages;

public class DefaultDatasetLayer extends AbstractLayer implements DatasetLayer {	

	private static final long serialVersionUID = 6966876644579947361L;
	
	private final Dataset dataset;
	
	public DefaultDatasetLayer(Dataset dataset) {
		super(dataset.getId(), false);
		
		this.dataset = dataset;
	}	

	@Override
	public String getSchemaName() {
		return dataset.getSchemaName();
	}

	@Override
	public String getTableName() {
		return dataset.getTableName();
	}

}
