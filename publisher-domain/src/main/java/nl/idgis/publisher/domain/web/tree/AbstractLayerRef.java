package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;

public abstract class AbstractLayerRef<T extends Layer> implements LayerRef<T>, Serializable {
	
	private static final long serialVersionUID = -6455525411189838962L;

	protected final T layer;
	
	AbstractLayerRef(T layer) {
		this.layer = layer;		
	}

	@Override
	public T getLayer() {
		return layer;
	}	

	@Override
	public GroupLayerRef asGroupRef() {
		throw new IllegalStateException("LayerRef is not a GroupLayerRef");
	}

	@Override
	public DatasetLayerRef asDatasetRef() {
		throw new IllegalStateException("LayerRef is not a DatasetLayerRef");
	}

}
