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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((child == null) ? 0 : child.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StructureItem other = (StructureItem) obj;
		if (child == null) {
			if (other.child != null)
				return false;
		} else if (!child.equals(other.child))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StructureItem [child=" + child + ", parent=" + parent + "]";
	}
}
