package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;
import java.util.Optional;

import nl.idgis.publisher.domain.web.tree.TilingSettings;

public abstract class EnsureLayer implements Serializable {

	private static final long serialVersionUID = -470937118284162135L;
	
	protected final String layerId, title, abstr;
	
	protected final TilingSettings tilingSettings;
	
	protected EnsureLayer(String layerId, String title, String abstr, TilingSettings tilingSettings) {
		this.layerId = layerId;
		this.title = title;
		this.abstr = abstr;
		this.tilingSettings = tilingSettings;
	}

	public String getLayerId() {
		return layerId;
	}

	public String getTitle() {
		return title;
	}

	public String getAbstract() {
		return abstr;
	}
	
	public Optional<TilingSettings> getTilingSettings() {
		return Optional.ofNullable(tilingSettings);
	}	
}
