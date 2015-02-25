package nl.idgis.publisher.domain.web.tree;

import java.util.List;
import java.util.Optional;

public class DefaultDatasetLayer extends AbstractLayer<Dataset> implements DatasetLayer {

	private static final long serialVersionUID = 873203510940749016L;

	public DefaultDatasetLayer(Dataset dataset) {
		super(dataset, false);
	}
	
	@Override
	public List<String> getKeywords() {
		return item.getKeywords();
	}
	
	@Override
	public String getTableName() {
		return item.getTableName();
	}
	
	@Override
	public Optional<Tiling> getTiling() {
		return item.getTiling();
	}

}
