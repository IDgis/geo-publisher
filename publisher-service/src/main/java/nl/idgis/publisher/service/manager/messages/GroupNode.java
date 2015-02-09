package nl.idgis.publisher.service.manager.messages;

public class GroupNode extends Node {

	private static final long serialVersionUID = 4098077788913605267L;

	public GroupNode(String id, String name, String title, String abstr) {
		super(id, name, title, abstr);
	}

	@Override
	public String toString() {
		return "Group [id=" + id + ", name=" + name + ", title=" + title
				+ ", abstr=" + abstr + "]";
	}
}
