package nl.idgis.publisher.domain.web.tree;

public class DefaultDatasetLayer extends AbstractLayer<Dataset> implements DatasetLayer {	

	private static final long serialVersionUID = -1629242928788272524L;

	public DefaultDatasetLayer(Dataset dataset) {
		super(dataset, false);
	}
	
	@Override
	public String getTableName() {
		return item.getTableName();
	}

}
