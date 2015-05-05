package nl.idgis.publisher.domain.web.tree;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultGroupLayer implements GroupLayer, Serializable {	

	private static final long serialVersionUID = -7304745349251033481L;

	private final PartialGroupLayer partialGroupLayer;

	private final Map<String, AbstractDatasetLayer> datasets;
	
	private final Map<String, PartialGroupLayer> groups;
	
	private final List<StructureItem> structure;
		
	public static DefaultGroupLayer newInstance(String groupId, List<AbstractDatasetLayer> datasets, List<PartialGroupLayer> groups, 
		List<StructureItem> structure) {
		
		Map<String, PartialGroupLayer> groupsMap = toMap(groups);
		if(groupsMap.containsKey(groupId)) {
			return new DefaultGroupLayer(groupsMap.get(groupId), toMap(datasets), groupsMap, structure);
		} else {
			throw new IllegalArgumentException("groupId not in groups list: " + groupId);
		}
	}
	
	DefaultGroupLayer(PartialGroupLayer partialGroupLayer, List<AbstractDatasetLayer> datasets, List<PartialGroupLayer> groups, 
		List<StructureItem> structure) {
		this(partialGroupLayer, toMap(datasets), toMap(groups), structure);
	}
	
	private DefaultGroupLayer(PartialGroupLayer partialGroupLayer, Map<String, AbstractDatasetLayer> datasets, Map<String, PartialGroupLayer> groups, 
		List<StructureItem> structure) {		
		
		this.partialGroupLayer = partialGroupLayer;
		this.datasets = datasets;
		this.groups = groups;
		this.structure = structure;		
	}
	
	private static <T extends AbstractLayer> Map<String, T> toMap(List<T> layers) {
		return layers.stream()
			.collect(Collectors.toMap(
				layer -> layer.getId(),
				Function.identity(),
				(a, b) -> a)); // list may contain duplicates
	}
	
	private LayerRef<?> asLayer(String id, Optional<StyleRef> styleRef) {
		if(datasets.containsKey(id)) {
			AbstractDatasetLayer datasetNode = datasets.get(id);			
			return new DefaultDatasetLayerRef(datasetNode, styleRef);
		}
		
		if(groups.containsKey(id)) {
			PartialGroupLayer partialGroupLayer = groups.get(id);			
			return new DefaultGroupLayerRef(new DefaultGroupLayer(partialGroupLayer, datasets, groups, structure));
		}
		
		throw new IllegalArgumentException("unknown layer id: " + id);
	}
	
	@Override
	public List<LayerRef<?>> getLayers() {
		return structure.stream()
			.filter(item -> getId().equals(item.getParent()))
			.map(groupEntry -> asLayer(groupEntry.getChild(), groupEntry.getStyleRef()))
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
	public boolean isConfidential () {
		List<LayerRef<?>> childLayerRefs = getLayers();
		for(final LayerRef<?> childLayerRef : childLayerRefs) {
			if(((Layer)childLayerRef.getLayer()).isConfidential()) {
				return true;
			}
		}
		
		return false;
	}
	
	private void toTree(StringBuilder sb, int depth) {
		for(LayerRef<? extends Layer> layerRef : getLayers()) {
			for(int i = 0; i < depth; i++) {
				sb.append("-");
			}
			
			sb.append(layerRef.getLayer().getName()).append("\n");
			
			if(layerRef.isGroupRef()) {
				((DefaultGroupLayer)layerRef.asGroupRef().getLayer()).toTree(sb, depth + 1);
			}
		}
	}
	
	public String toTree() {
		StringBuilder sb = new StringBuilder();
		toTree(sb, 0);
		return sb.toString();
	}

	@Override
	public String toString() {
		return "DefaultGroupLayer [partialGroupLayer=" + partialGroupLayer
				+ ", datasets=" + datasets + ", groups=" + groups
				+ ", structure=" + structure + "]";
	}

		
}
