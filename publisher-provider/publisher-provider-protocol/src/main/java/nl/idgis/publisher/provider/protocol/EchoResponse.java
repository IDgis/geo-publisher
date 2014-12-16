package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

public class EchoResponse implements Serializable {

	private static final long serialVersionUID = 456102029330133757L;
	
	private final Object payload;
	
	public EchoResponse(Object payload) {
		this.payload = payload;
	}
	
	public Object getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		return "EchoResponse [payload=" + payload + "]";
	}
}
