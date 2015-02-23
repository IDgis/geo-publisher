package nl.idgis.publisher.domain.web.tree;

import com.mysema.query.annotations.QueryProjection;

public class GroupNode extends Node implements Group {

	private static final long serialVersionUID = 7986334970024624615L;

	@QueryProjection
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
