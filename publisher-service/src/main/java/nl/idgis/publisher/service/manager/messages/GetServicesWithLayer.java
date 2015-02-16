package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

public class GetServicesWithLayer implements Serializable {	
	
	private static final long serialVersionUID = -8377707562683452781L;
	
	private final String layerId;
	
	public GetServicesWithLayer(String layerId) {
		this.layerId = layerId;
	}
	
	public String getLayerId() {
		return layerId;
	}

	@Override
	public String toString() {
		return "GetServicesWithLayer [layerId=" + layerId + "]";
	}
}
