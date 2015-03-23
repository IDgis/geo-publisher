package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;

public class DefaultStyleRef implements StyleRef, Serializable {	

	private static final long serialVersionUID = -1004305351206527393L;
	
	private final String id;
	
	private final String name;
	
	public DefaultStyleRef(String id, String name) {
		this.id = id;
		this.name = name;
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {		
		return name;
	}

	@Override
	public String toString() {
		return "DefaultStyle [id=" + id + ", name=" + name + "]";
	}
	
}
