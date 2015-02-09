package nl.idgis.publisher.service.manager.messages;

public class DefaultDatasetLayer extends AbstractLayer implements DatasetLayer {	

	private static final long serialVersionUID = 6966876644579947361L;
	
	private final DatasetNode dataset;
	
	public DefaultDatasetLayer(DatasetNode dataset) {
		super(false);
		
		this.dataset = dataset;
	}
	
	@Override
	public String getId() {
		return dataset.getId();
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
