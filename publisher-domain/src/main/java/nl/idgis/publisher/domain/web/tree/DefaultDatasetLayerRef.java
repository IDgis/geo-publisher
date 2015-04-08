package nl.idgis.publisher.domain.web.tree;

public class DefaultDatasetLayerRef extends AbstractLayerRef<DatasetLayer> implements DatasetLayerRef {	

	private static final long serialVersionUID = 5328262821783245084L;
	
	private final StyleRef styleRef;

	public DefaultDatasetLayerRef(AbstractDatasetLayer layer, StyleRef styleRef) {
		super(layer);
		
		this.styleRef = styleRef;
	}
	
	@Override
	public boolean isGroupRef() {
		return false;
	}

	@Override
	public StyleRef getStyleRef() {
		return styleRef;
	}
	
	@Override
	public DatasetLayerRef asDatasetRef() {
		return this;
	}

	@Override
	public String toString() {
		return "DefaultDatasetLayerRef [styleRef=" + styleRef + ", layer=" + layer
				+ "]";
	}
}
