package nl.idgis.publisher.domain.web.tree;

import com.mysema.query.annotations.QueryProjection;

public class GroupNode extends Node implements Group {

	private static final long serialVersionUID = 7986334970024624615L;

	@QueryProjection
	public GroupNode(String id, String name, String title, String abstr) {
		super(id, name, title, abstr);
	}

	@Override
	public String toString() {
		return "Group [id=" + id + ", name=" + name + ", title=" + title
				+ ", abstr=" + abstr + "]";
	}
}
