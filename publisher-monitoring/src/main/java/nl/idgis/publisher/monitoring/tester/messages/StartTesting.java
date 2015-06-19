package nl.idgis.publisher.monitoring.tester.messages;

import java.io.Serializable;

public class StartTesting implements Serializable {
	
	private static final long serialVersionUID = 1627487921522206841L;
	
	private boolean discardPending;
	
	public StartTesting(boolean discardPending) {
		this.discardPending = discardPending;
	}
	
	public boolean getDiscardPending() {
		return this.discardPending;
	}

	@Override
	public String toString() {
		return "StartTesting [discardPending=" + discardPending + "]";
	}	

}
