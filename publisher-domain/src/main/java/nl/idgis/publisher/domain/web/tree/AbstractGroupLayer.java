package nl.idgis.publisher.domain.web.tree;

import nl.idgis.publisher.utils.Lazy;

import java.io.Serializable;
import java.util.List;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Optional;

public abstract class AbstractGroupLayer implements GroupLayer, Serializable {
	
	private static final long serialVersionUID = -5053415862522039499L;
	
	private final Lazy<Boolean> confidential;
	
	private final Lazy<Optional<Timestamp>> importTime;
	
	public AbstractGroupLayer() {
		confidential = new Lazy<>(() -> {
			List<LayerRef<? extends Layer>> childLayerRefs = getLayers();
			for(final LayerRef<?> childLayerRef : childLayerRefs) {
				if(childLayerRef.getLayer().isConfidential()) {
					return true;
				}
			}
			
			return false;
		});
		
		importTime = new Lazy<>(() ->
			getLayers().stream()
				.map(layerRef -> layerRef.getLayer().getImportTime())
				.filter(Optional::isPresent)
				.map(Optional::get)
				.max(Comparator.naturalOrder()));
	}

	@Override
	public boolean isConfidential () {
		return confidential.get();
	}
	
	@Override
	public Optional<Timestamp> getImportTime() {
		return importTime.get();
	}
}
