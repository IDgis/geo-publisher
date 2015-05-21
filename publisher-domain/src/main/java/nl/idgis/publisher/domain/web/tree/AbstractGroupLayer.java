package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.List;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Optional;

public abstract class AbstractGroupLayer implements GroupLayer, Serializable {
	
	private static final long serialVersionUID = -5053415862522039499L;

	@Override
	public boolean isConfidential () {
		List<LayerRef<? extends Layer>> childLayerRefs = getLayers();
		for(final LayerRef<?> childLayerRef : childLayerRefs) {
			if(childLayerRef.getLayer().isConfidential()) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public Optional<Timestamp> getImportTime() {
		return getLayers().stream()
			.map(layerRef -> layerRef.getLayer().getImportTime())
			.filter(Optional::isPresent)
			.map(Optional::get)
			.max(Comparator.naturalOrder());
	}
}
