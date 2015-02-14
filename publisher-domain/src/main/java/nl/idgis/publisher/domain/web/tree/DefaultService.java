package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class DefaultService implements Service, Serializable {	

	private static final long serialVersionUID = -4657847579083869249L;

	private final String id, name;
	
	private final GroupLayer root;
	
	public DefaultService(String id, String name, GroupNode root, List<DatasetNode> datasets, List<GroupNode> groups, Map<String, String> structure) {
		this.id = id;
		this.name = name;
		this.root = new DefaultGroupLayer(root, datasets, groups, structure);
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
	public String getRootId() {
		return root.getId();
	}

	@Override
	public List<Layer> getLayers() {
		return root.getLayers();
	}
}
