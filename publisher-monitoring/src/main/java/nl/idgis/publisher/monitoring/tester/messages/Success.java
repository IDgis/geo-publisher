package nl.idgis.publisher.monitoring.tester.messages;

import java.net.URL;

public class Success extends Result {
	
	private static final long serialVersionUID = -8328105037085970517L;

	public Success(URL url) {
		super(url);
	}

	@Override
	public String toString() {
		return "Success []";
	}
	
}
