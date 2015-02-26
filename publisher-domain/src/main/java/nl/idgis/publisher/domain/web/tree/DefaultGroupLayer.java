package nl.idgis.publisher.domain.web.tree;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultGroupLayer extends AbstractLayer implements GroupLayer {	

	private static final long serialVersionUID = 4552928977448464315L;

	private final Map<String, DatasetNode> datasets;
	
	private final Map<String, GroupNode> groups;
	
	private final Map<String, String> structure;
	
	private final Map<String, String> styles;
	
	public DefaultGroupLayer(GroupNode group, List<DatasetNode> datasets, List<GroupNode> groups, 
		Map<String, String> structure, Map<String, String> styles) {
		this(group, toMap(datasets), toMap(groups), structure, styles);
	}
	
	private DefaultGroupLayer(GroupNode group, Map<String, DatasetNode> datasets, Map<String, GroupNode> groups, 
		Map<String, String> structure, Map<String, String> styles) {		
		super(
			group.getId(), 
			group.getName(), 
			group.getTitle(), 
			group.getAbstract(), 
			group.getTiling().orElse(null));

		this.datasets = datasets;
		this.groups = groups;
		this.structure = structure;
		this.styles = styles;
	}
	
	private static <T extends AbstractLayer> Map<String, T> toMap(List<T> list) {
		return list.stream()
			.collect(Collectors.toMap(
				item -> item.getId(), 
				item -> item));
	}
	
	private LayerRef<?> asLayer(String id) {
		if(datasets.containsKey(id)) {
			DatasetNode datasetNode = datasets.get(id);
			
			return new DefaultDatasetLayerRef(new DefaultDatasetLayer(datasetNode), styles.get(id));
		}
		
		if(groups.containsKey(id)) {
			GroupNode groupNode = groups.get(id);
			
			return new DefaultGroupLayerRef(new DefaultGroupLayer(groupNode, datasets, groups, structure, styles));
		}
		
		throw new IllegalArgumentException("unknown layer id: " + id);
	}
	
	protected boolean filterGroup(Map.Entry<String, String> groupEntry) {
		return getId().equals(groupEntry.getValue());
	}

	@Override
	public List<LayerRef<?>> getLayers() {
		return structure.entrySet().stream()
			.filter(this::filterGroup)
			.map(groupEntry -> asLayer(groupEntry.getKey()))
			.collect(Collectors.toList());
	}	
}
