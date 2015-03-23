package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;

public class StructureItem implements Serializable {

	private static final long serialVersionUID = 7208268243004698224L;
	
	private final String child, parent;
	
	public StructureItem(String child, String parent) {
		this.child = child;
		this.parent = parent;
	}

	public String getChild() {
		return child;
	}

	public String getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return "StructureItem [child=" + child + ", parent=" + parent + "]";
	}
}
