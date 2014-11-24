package nl.idgis.publisher.xml.exceptions;

import com.google.common.collect.BiMap;

public class MultipleNodes extends QueryFailure {
	
	private static final long serialVersionUID = -1905885025071791800L;

	public MultipleNodes(BiMap<String, String> namespaces, String path) {
		super(namespaces, path);
	}
	
	public MultipleNodes(BiMap<String, String> namespaces, String path, Throwable cause) {
		super(namespaces, path, cause);
	}

	@Override
	public String toString() {
		return "MultipleNodes [namespaces=" + namespaces + ", path=" + path
				+ "]";
	}	

}
