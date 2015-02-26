package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public class DefaultDatasetLayer extends AbstractLayer implements DatasetLayer {

	private static final long serialVersionUID = 873203510940749016L;
	
	private final List<String> keywords;
	
	private final String tableName;
	
	private final List<String> styles;

	public DefaultDatasetLayer(DatasetNode datasetNode) {		
		super(
			datasetNode.getId(), 
			datasetNode.getName(), 
			datasetNode.getTitle(), 
			datasetNode.getAbstract(), 
			datasetNode.getTiling().orElse(null));
		
		this.keywords = datasetNode.getKeywords();
		this.tableName = datasetNode.getTableName();
		this.styles = datasetNode.getStyles();
	}

	@Override
	public List<String> getKeywords() {
		return keywords;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public List<String> getStyles() {
		return styles;
	}

}
