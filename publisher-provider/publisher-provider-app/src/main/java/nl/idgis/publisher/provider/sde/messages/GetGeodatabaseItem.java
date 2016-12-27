package nl.idgis.publisher.provider.sde.messages;

import java.io.Serializable;

public class GetGeodatabaseItem implements Serializable {

	private static final long serialVersionUID = -7505514698111055310L;
	
	private final String uuid;
	
	public GetGeodatabaseItem(String uuid) {
		this.uuid = uuid;
	}
	
	public String getUuid() {
		return uuid;
	}

	@Override
	public String toString() {
		return "GetGeodatabaseItem [uuid=" + uuid + "]";
	}
}
