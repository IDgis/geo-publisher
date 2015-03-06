package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

public class GetServicesWithStyle implements Serializable {

	private static final long serialVersionUID = 5806865390147705621L;
	
	private final String styleId;
	
	public GetServicesWithStyle(String styleId) {
		this.styleId = styleId;
	}
	
	public String getStyleId() {
		return styleId;
	}

	@Override
	public String toString() {
		return "GetServicesWithStyle [styleId=" + styleId + "]";
	}
}
