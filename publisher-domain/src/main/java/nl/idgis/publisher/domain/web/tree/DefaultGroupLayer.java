package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultGroupLayer implements GroupLayer, Serializable {	

	private static final long serialVersionUID = -7304745349251033481L;

	private final PartialGroupLayer partialGroupLayer;

	private final Map<String, DefaultDatasetLayer> datasets;
	
	private final Map<String, PartialGroupLayer> groups;
	
	private final Map<String, String> structure;
	
	private final Map<String, String> styles;
	
	public static DefaultGroupLayer newInstance(String groupId, List<DefaultDatasetLayer> datasets, List<PartialGroupLayer> groups, 
		Map<String, String> structure, Map<String, String> styles) {
		
		Map<String, PartialGroupLayer> groupsMap = toMap(groups);
		return new DefaultGroupLayer(groupsMap.get(groupId), toMap(datasets), groupsMap, structure, styles);
	}
	
	DefaultGroupLayer(PartialGroupLayer partialGroupLayer, List<DefaultDatasetLayer> datasets, List<PartialGroupLayer> groups, 
		Map<String, String> structure, Map<String, String> styles) {
		this(partialGroupLayer, toMap(datasets), toMap(groups), structure, styles);
	}
	
	private DefaultGroupLayer(PartialGroupLayer partialGroupLayer, Map<String, DefaultDatasetLayer> datasets, Map<String, PartialGroupLayer> groups, 
		Map<String, String> structure, Map<String, String> styles) {		
		
		this.partialGroupLayer = partialGroupLayer;
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
			DefaultDatasetLayer datasetNode = datasets.get(id);			
			return new DefaultDatasetLayerRef(datasetNode, styles.get(id));
		}
		
		if(groups.containsKey(id)) {
			PartialGroupLayer partialGroupLayer = groups.get(id);			
			return new DefaultGroupLayerRef(new DefaultGroupLayer(partialGroupLayer, datasets, groups, structure, styles));
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

	@Override
	public String getId() {
		return partialGroupLayer.getId();
	}

	@Override
	public String getName() {
		return partialGroupLayer.getName();
	}

	@Override
	public String getTitle() {
		return partialGroupLayer.getTitle();
	}

	@Override
	public String getAbstract() {
		return partialGroupLayer.getAbstract();
	}

	@Override
	public Optional<Tiling> getTiling() {
		return partialGroupLayer.getTiling();
	}

	@Override
	public String toString() {
		return "DefaultGroupLayer [partialGroupLayer=" + partialGroupLayer + ", datasets=" + datasets
				+ ", groups=" + groups + ", structure=" + structure
				+ ", styles=" + styles + "]";
	}	
}
