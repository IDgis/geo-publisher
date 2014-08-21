package nl.idgis.publisher.xml.messages;

import java.io.Serializable;

public class NotParseable implements Serializable {

	private static final long serialVersionUID = -4962047163990747282L;
	
	private final String reason;

	public NotParseable(String reason) {
		this.reason = reason;
	}
	
	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		return "NotParseable [reason=" + reason + "]";
	}
}
