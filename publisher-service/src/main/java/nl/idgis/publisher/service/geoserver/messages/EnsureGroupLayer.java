package nl.idgis.publisher.service.geoserver.messages;

import java.util.List;

import nl.idgis.publisher.service.geoserver.rest.LayerGroup;
import nl.idgis.publisher.service.geoserver.rest.LayerRef;

public class EnsureGroupLayer extends EnsureLayer {

	private static final long serialVersionUID = 3915345897544846786L;

	public EnsureGroupLayer(String layerId, String title, String abstr) {
		super(layerId, title, abstr);
	}
	
	public LayerGroup getLayerGroup(List<LayerRef> groupLayerContent) {
		return new LayerGroup(
				layerId,
				title,
				abstr,
				groupLayerContent);
	}
}
