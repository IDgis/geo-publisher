package nl.idgis.publisher.service.manager.messages;

public class DefaultDatasetLayer extends AbstractLayer<Dataset> implements DatasetLayer {	
	
	private static final long serialVersionUID = 5033718583098886699L;
	
	public DefaultDatasetLayer(Dataset dataset) {
		super(dataset, false);
	}
	
	@Override
	public String getTableName() {
		return item.getTableName();
	}

}
