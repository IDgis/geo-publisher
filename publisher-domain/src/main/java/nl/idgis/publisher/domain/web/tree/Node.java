package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;

public abstract class Node implements Item, Serializable {
	
	private static final long serialVersionUID = 3164923466251389875L;
	
	protected final String id, name, title, abstr;
	
	public Node(String id, String name, String title, String abstr) {
		this.id = id;
		this.name = name;
		this.title = title;
		this.abstr = abstr;
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
	public String getTitle() {
		return title;
	}

	@Override
	public String getAbstract() {
		return abstr;
	}
}
