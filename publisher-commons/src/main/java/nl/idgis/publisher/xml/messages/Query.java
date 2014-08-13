package nl.idgis.publisher.xml.messages;

import java.io.Serializable;

import com.google.common.collect.BiMap;

public abstract class Query implements Serializable {
	
	private static final long serialVersionUID = -9004333251178891052L;
	
	protected final BiMap<String, String> namespaces;
	protected final String expression;
	
	public Query(BiMap<String, String> namespaces, String path) {
		this.namespaces = namespaces;
		this.expression = path;
	}

	public BiMap<String, String> getNamespaces() {
		return namespaces;
	}

	public String getExpression() {
		return expression;
	}
	
}
