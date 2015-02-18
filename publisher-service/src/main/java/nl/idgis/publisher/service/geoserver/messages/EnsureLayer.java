package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;

public abstract class EnsureLayer implements Serializable {

	private static final long serialVersionUID = -470937118284162135L;
	
	protected final String layerId, title, abstr;
	
	protected EnsureLayer(String layerId, String title, String abstr) {
		this.layerId = layerId;
		this.title = title;
		this.abstr = abstr;
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

	@Override
	public String toString() {
		return "EnsureLayer [layerId=" + layerId + ", title=" + title
				+ ", abstr=" + abstr + "]";
	}
}
