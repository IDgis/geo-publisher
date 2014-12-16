package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

public class EchoRequest implements Serializable {
	
	private static final long serialVersionUID = -3830529810102152091L;
	
	private final Object payload;
	
	public EchoRequest(Object payload) {
		this.payload = payload;
	}
	
	public Object getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		return "EchoRequest [payload=" + payload + "]";
	}
}
