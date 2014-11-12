package nl.idgis.publisher.xml.exceptions;

import com.google.common.collect.BiMap;

public abstract class QueryFailure extends Exception {	
	
	private static final long serialVersionUID = 5791752083284611534L;
	
	protected final BiMap<String, String> namespaces;
	protected final String path;	
	
	public QueryFailure(BiMap<String, String> namespaces, String path) {
		this(namespaces, path, null);
	}

	public QueryFailure(BiMap<String, String> namespaces, String path, Throwable cause) {
		super(cause);
		
		this.namespaces = namespaces;
		this.path = path;
	}

	public BiMap<String, String> getNamespaces() {
		return namespaces;
	}

	public String getPath() {
		return path;
	}
	
}