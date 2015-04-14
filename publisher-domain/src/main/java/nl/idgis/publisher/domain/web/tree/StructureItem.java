package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.Optional;

public class StructureItem implements Serializable {	

	private static final long serialVersionUID = -6473857595338113180L;

	private final String child, parent;
	
	private final StyleRef styleRef;
	
	public StructureItem(String child, String parent, Optional<StyleRef> styleRef) {
		this.child = child;
		this.parent = parent;
		this.styleRef = styleRef.orElse(null);
	}

	public String getChild() {
		return child;
	}

	public String getParent() {
		return parent;
	}
	
	public Optional<StyleRef> getStyleRef() {
		return Optional.ofNullable(styleRef);
	}

	@Override
	public String toString() {
		return "StructureItem [child=" + child + ", parent=" + parent
				+ ", styleRef=" + styleRef + "]";
	}	
}
