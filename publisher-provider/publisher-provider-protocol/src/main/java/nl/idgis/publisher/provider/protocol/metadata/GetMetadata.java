package nl.idgis.publisher.provider.protocol.metadata;

import java.io.Serializable;

import nl.idgis.publisher.stream.messages.Start;

public class GetMetadata extends Start implements Serializable {
	
	private static final long serialVersionUID = -8610001910316843276L;

	@Override
	public String toString() {
		return "GetMetadata []";
	}
	
}
