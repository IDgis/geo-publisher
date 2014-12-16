package nl.idgis.publisher.provider.metadata.messages;

import java.io.Serializable;

public class GetMetadata implements Serializable {
	
	private static final long serialVersionUID = -5754311358265214678L;
	
	private final String identification;
	
	public GetMetadata(String identification) {
		this.identification = identification;
	}

	public String getIdentification() {
		return identification;
	}

	@Override
	public String toString() {
		return "GetMetadata [identification=" + identification + "]";
	}
}
