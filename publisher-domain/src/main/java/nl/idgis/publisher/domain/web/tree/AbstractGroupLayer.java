package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.List;

public abstract class AbstractGroupLayer implements GroupLayer, Serializable {
	
	private static final long serialVersionUID = -5053415862522039499L;

	@Override
	public boolean isConfidential () {
		List<LayerRef<?>> childLayerRefs = getLayers();
		for(final LayerRef<?> childLayerRef : childLayerRefs) {
			if(((Layer)childLayerRef.getLayer()).isConfidential()) {
				return true;
			}
		}
		
		return false;
	}
}
