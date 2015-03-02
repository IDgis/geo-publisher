package nl.idgis.publisher.domain.web.tree;

public class DefaultDatasetLayerRef extends AbstractLayerRef<DatasetLayer> implements DatasetLayerRef {	

	private static final long serialVersionUID = -536475503267823183L;
	
	private final String style;

	public DefaultDatasetLayerRef(DefaultDatasetLayer layer, String style) {
		super(layer);
		
		this.style = style;
	}
	
	@Override
	public boolean isGroupRef() {
		return false;
	}

	@Override
	public String getStyleName() {
		return style;
	}
	
	@Override
	public DatasetLayerRef asDatasetRef() {
		return this;
	}

	@Override
	public String toString() {
		return "DefaultDatasetLayerRef [style=" + style + ", layer=" + layer
				+ "]";
	}
}
