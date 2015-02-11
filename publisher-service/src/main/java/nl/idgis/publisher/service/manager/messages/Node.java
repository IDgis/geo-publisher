package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

public abstract class Node implements Item, Serializable {

	private static final long serialVersionUID = -1690842575697161436L;
	
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
	public String getAbstr() {
		return abstr;
	}
}
