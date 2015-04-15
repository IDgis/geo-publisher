package nl.idgis.publisher.domain.web.tree;

import java.util.Optional;

public class DefaultDatasetLayerRef extends AbstractLayerRef<DatasetLayer> implements DatasetLayerRef {	

	private static final long serialVersionUID = 5328262821783245084L;
	
	private final StyleRef styleRef;

	public DefaultDatasetLayerRef(AbstractDatasetLayer layer, Optional<StyleRef> styleRef) {
		super(layer);
		
		this.styleRef = styleRef.orElse(null);
	}
	
	@Override
	public boolean isGroupRef() {
		return false;
	}

	@Override
	public Optional<StyleRef> getStyleRef() {
		return Optional.ofNullable(styleRef);
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
