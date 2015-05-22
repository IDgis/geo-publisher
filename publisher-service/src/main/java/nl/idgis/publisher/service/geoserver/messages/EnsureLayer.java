package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

import nl.idgis.publisher.domain.web.tree.Tiling;

import nl.idgis.publisher.service.geoserver.rest.TiledLayer;
import nl.idgis.publisher.service.geoserver.rest.GridSubset;

public abstract class EnsureLayer implements Serializable {

	private static final long serialVersionUID = -470937118284162135L;
	
	protected final String layerId, title, abstr;
	
	protected final Tiling tilingSettings;
	
	protected final boolean reimported;
	
	protected EnsureLayer(String layerId, String title, String abstr, Tiling tilingSettings, boolean reimported) {
		this.layerId = layerId;
		this.title = title;
		this.abstr = abstr;		
		this.tilingSettings = tilingSettings;
		this.reimported = reimported;
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
	
	public boolean isReimported() {
		return reimported;
	}
	
	public Optional<TiledLayer> getTiledLayer() {
		return Optional.ofNullable(tilingSettings).map(tilingSettings -> 
			new TiledLayer(
				tilingSettings.getMimeFormats(),
				Arrays.asList(new GridSubset(
					"urn:ogc:def:wkss:OGC:1.0:NLDEPSG28992Scale",
					Optional.empty(), // minCachedLevel
					Optional.of(11))), // maxCachedLevel
				tilingSettings.getMetaWidth(),
				tilingSettings.getMetaHeight(),
				tilingSettings.getExpireCache(),
				tilingSettings.getExpireClients(),
				tilingSettings.getGutter()));
	}
}
