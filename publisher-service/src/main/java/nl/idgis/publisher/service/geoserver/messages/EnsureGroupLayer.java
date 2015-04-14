package nl.idgis.publisher.service.geoserver.messages;

import java.util.List;

import nl.idgis.publisher.domain.web.tree.Tiling;

import nl.idgis.publisher.service.geoserver.rest.LayerGroup;
import nl.idgis.publisher.service.geoserver.rest.PublishedRef;

public class EnsureGroupLayer extends EnsureLayer {

	private static final long serialVersionUID = 7394934345489321332L;

	public EnsureGroupLayer(String layerId, String title, String abstr, Tiling tilingSettings) {
		super(layerId, title, abstr, tilingSettings);
	}
	
	public LayerGroup getLayerGroup(List<PublishedRef> groupLayerContent) {
		return new LayerGroup(
				layerId,
				title,
				abstr,
				groupLayerContent);
	}

	@Override
	public String toString() {
		return "EnsureGroupLayer [layerId=" + layerId + ", title=" + title
				+ ", abstr=" + abstr + ", tilingSettings=" + tilingSettings
				+ "]";
	}
}
