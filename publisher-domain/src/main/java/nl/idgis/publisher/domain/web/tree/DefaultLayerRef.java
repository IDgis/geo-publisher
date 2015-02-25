package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;

public class DefaultLayerRef implements LayerRef, Serializable {	

	private static final long serialVersionUID = 2750355324299502882L;

	private final Layer layer;
	
	private final String style;
	
	public DefaultLayerRef(Layer layer, String style) {
		this.layer = layer;
		this.style = style;
	}

	@Override
	public Layer getLayer() {
		return layer;
	}

	@Override
	public String getStyle() {
		return style;
	}

	@Override
	public String toString() {
		return "DefaultLayerRef [layer=" + layer + ", style=" + style + "]";
	}

}
