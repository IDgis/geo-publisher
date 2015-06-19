package nl.idgis.publisher.monitoring.tester.messages;

import java.io.Serializable;
import java.net.URL;
import java.util.Objects;

public class AddUrl implements Serializable {
	
	private static final long serialVersionUID = 6561177073693050811L;
	
	private final URL url;
	
	public AddUrl(URL url) {
		this.url = Objects.requireNonNull(url);
	}

	public URL getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return "AddUrl [url=" + url + "]";
	}
}
