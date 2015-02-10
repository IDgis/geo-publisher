package nl.idgis.publisher.service.manager.messages;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultGroupLayer extends AbstractLayer implements GroupLayer {
	
	private static final long serialVersionUID = -5112614407998270385L;
	
	private final GroupNode group;

	private final Map<String, DatasetNode> datasets;
	
	private final Map<String, GroupNode> groups;
	
	private final Map<String, String> structure;
	
	public DefaultGroupLayer(GroupNode group, Map<String, DatasetNode> datasets, Map<String, GroupNode> groups, Map<String, String> structure) {
		super(true);
		
		this.group = group;
		this.datasets = datasets;
		this.groups = groups;
		this.structure = structure;
	}
	
	@Override
	public String getId() {
		return group.getId();
	}	
	
	private Layer asLayer(String id) {
		if(datasets.containsKey(id)) {
			return new DefaultDatasetLayer(datasets.get(id));
		}
		
		if(groups.containsKey(id)) {
			return new DefaultGroupLayer(groups.get(id), datasets, groups, structure);
		}
		
		throw new IllegalArgumentException("unknown layer id: " + id);
	}
	
	protected boolean filterGroup(Map.Entry<String, String> groupEntry) {
		return getId().equals(groupEntry.getValue());
	}

	@Override
	public List<Layer> getLayers() {
		return structure.entrySet().stream()
			.filter(this::filterGroup)
			.map(groupEntry -> asLayer(groupEntry.getKey()))
			.collect(Collectors.toList());
	}	
}
