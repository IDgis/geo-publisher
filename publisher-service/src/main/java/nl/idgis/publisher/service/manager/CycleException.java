package nl.idgis.publisher.service.manager;

public class CycleException extends Exception {

	private static final long serialVersionUID = 1113699209056554330L;

	private final String layerId;
	
	public CycleException(String layerId) {
		this.layerId = layerId;
	}

	public String getLayerId() {
		return layerId;
	}

	
}
