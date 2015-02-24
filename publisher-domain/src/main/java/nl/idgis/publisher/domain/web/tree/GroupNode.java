package nl.idgis.publisher.domain.web.tree;

public class GroupNode extends Node implements Group {

	private static final long serialVersionUID = 7986334970024624615L;

	public GroupNode(String id, String name, String title, String abstr, TilingSettings tilingSettings) {
		super(id, name, title, abstr, tilingSettings);
	}

	@Override
	public String toString() {
		return "GroupNode [id=" + id + ", name=" + name + ", title=" + title
				+ ", abstr=" + abstr + ", tilingSettings=" + tilingSettings
				+ "]";
	}
	
}
