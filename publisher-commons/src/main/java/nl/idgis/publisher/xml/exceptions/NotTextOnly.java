package nl.idgis.publisher.xml.exceptions;

import com.google.common.collect.BiMap;

public class NotTextOnly extends QueryFailure {
	
	private static final long serialVersionUID = 3873842166982056091L;

	public NotTextOnly(BiMap<String, String> namespaces, String path) {
		super(namespaces, path);		
	}	

	public NotTextOnly(BiMap<String, String> namespaces, String path, Throwable cause) {
		super(namespaces, path, cause);		
	}

}
