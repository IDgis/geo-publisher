package nl.idgis.publisher.domain.web.tree;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultGroupLayer extends AbstractLayer<Group> implements GroupLayer {	

	private static final long serialVersionUID = 4552928977448464315L;

	private final Map<String, DatasetNode> datasets;
	
	private final Map<String, GroupNode> groups;
	
	private final Map<String, String> structure;
	
	private final Map<String, String> styles;
	
	public DefaultGroupLayer(Group group, List<DatasetNode> datasets, List<GroupNode> groups, 
		Map<String, String> structure, Map<String, String> styles) {
		this(group, toMap(datasets), toMap(groups), structure, styles);
	}
	
	private DefaultGroupLayer(Group group, Map<String, DatasetNode> datasets, Map<String, GroupNode> groups, 
		Map<String, String> structure, Map<String, String> styles) {		
		super(group, true);
		this.datasets = datasets;
		this.groups = groups;
		this.structure = structure;
		this.styles = styles;
	}
	
	private static <T extends Node> Map<String, T> toMap(List<T> list) {
		return list.stream()
			.collect(Collectors.toMap(
				item -> item.getId(), 
				item -> item));
	}
	
	private LayerRef asLayer(String id) {
		if(datasets.containsKey(id)) {
			return new DefaultLayerRef(new DefaultDatasetLayer(datasets.get(id)), styles.get(id));
		}
		
		if(groups.containsKey(id)) {
			return new DefaultLayerRef(new DefaultGroupLayer(groups.get(id), datasets, groups, structure, styles), null);
		}
		
		throw new IllegalArgumentException("unknown layer id: " + id);
	}
	
	protected boolean filterGroup(Map.Entry<String, String> groupEntry) {
		return getId().equals(groupEntry.getValue());
	}

	@Override
	public List<LayerRef> getLayers() {
		return structure.entrySet().stream()
			.filter(this::filterGroup)
			.map(groupEntry -> asLayer(groupEntry.getKey()))
			.collect(Collectors.toList());
	}	
}
