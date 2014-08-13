package nl.idgis.publisher.xml.messages;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class GetString extends Query {
	
	private static final long serialVersionUID = 1391913298495107583L;
	
	public GetString(String path) {
		this(HashBiMap.<String, String>create(), path);
	}

	public GetString(BiMap<String, String> namespaces, String path) {
		super(namespaces, path);
	}

	@Override
	public String toString() {
		return "GetString [namespaces=" + namespaces + ", path=" + expression + "]";
	}
	
}
