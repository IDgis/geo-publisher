package nl.idgis.publisher.monitoring.tester.messages;

import java.io.Serializable;
import java.net.URL;
import java.util.Objects;

public abstract class Result implements Serializable {
	
	private static final long serialVersionUID = 8384522038550356595L;
	
	protected final URL url;
	
	protected Result(URL url) {
		this.url = Objects.requireNonNull(url);
	}
	
	public URL getUrl() {
		return url;
	}
}
