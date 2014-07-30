package nl.idgis.publisher.domain.log;

import java.io.Serializable;

public abstract class LogLine implements Serializable {
	
	private static final long serialVersionUID = 7393703558996721495L;
	
	private final Enum<?> event;
	
	protected LogLine(Enum<?> event) {
		this.event = event;
	}
	
	public Enum<?> getEvent() {
		return event;
	}
}
