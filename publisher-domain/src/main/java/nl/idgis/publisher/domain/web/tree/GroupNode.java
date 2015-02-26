package nl.idgis.publisher.domain.web.tree;

public class GroupNode extends AbstractLayer {

	private static final long serialVersionUID = 2592638371421634222L;

	public GroupNode(String id, String name, String title, String abstr, Tiling tiling) {
		super(id, name, title, abstr, tiling);
	}

	@Override
	public String toString() {
		return "GroupNode [id=" + id + ", name=" + name + ", title=" + title
				+ ", abstr=" + abstr + ", tiling=" + tiling
				+ "]";
	}
	
}
