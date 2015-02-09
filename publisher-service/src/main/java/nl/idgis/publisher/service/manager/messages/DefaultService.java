package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultService implements Service, Serializable {

	private static final long serialVersionUID = 635298313132926547L;
	
	private final String id;
	
	private final GroupLayer root;
	
	public DefaultService(String id, GroupNode root, List<DatasetNode> datasets, List<GroupNode> groups, Map<String, String> structure) {
		this.id = id;
		this.root = new DefaultGroupLayer(root, toMap(datasets), toMap(groups), structure);
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public String getRootId() {
		return root.getId();
	}
	
	private <T extends Node> Map<String, T> toMap(List<T> list) {
		return list.stream()
			.collect(Collectors.toMap(
				item -> item.getId(), 
				item -> item));
	}

	@Override
	public List<Layer> getLayers() {
		return root.getLayers();
	}
}
