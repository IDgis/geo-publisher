package nl.idgis.publisher.xml.exceptions;

import com.google.common.collect.BiMap;

public class NotFound extends QueryFailure {

	private static final long serialVersionUID = 741681509457855147L;

	public NotFound(BiMap<String, String> namespaces, String path) {
		super(namespaces, path);
	}	

	public NotFound(BiMap<String, String> namespaces, String path, Throwable cause) {
		super(namespaces, path, cause);
	}

	@Override
	public String toString() {
		return "NotFound [namespaces=" + namespaces + ", path=" + path + "]";
	}
	
}
