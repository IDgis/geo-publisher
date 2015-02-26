package nl.idgis.publisher.domain.web.tree;

public interface LayerRef<T extends Layer> {
	
	T getLayer();
	
	boolean isGroupRef();
	
	GroupLayerRef asGroupRef();
	
	DatasetLayerRef asDatasetRef();
}
