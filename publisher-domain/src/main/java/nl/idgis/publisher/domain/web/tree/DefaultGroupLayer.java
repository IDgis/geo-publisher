package nl.idgis.publisher.domain.web.tree;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import nl.idgis.publisher.utils.GZIPSerializer;

public class DefaultGroupLayer extends AbstractGroupLayer {
	
	private static final long serialVersionUID = -8006116069339954358L;
	
	private static final GZIPSerializer<DefaultGroupLayer> serializer = new GZIPSerializer<>(DefaultGroupLayer.class);
	
	private final PartialGroupLayer partialGroupLayer;
	
	/**
	 * A map containing all {@link AbstractDatasetLayer} of the tree, may contain
	 * items that are not contained by this group.
	 */
	private final Map<String, AbstractDatasetLayer> datasets;
	
	/**
	 * A map containing all {@link PartialGroupLayer} of the tree, may contain
	 * items that are not contained by this group.
	 */
	private final Map<String, PartialGroupLayer> groups;
	
	/**
	 * A list of {@link StructureItem} describing the tree structure, may 
	 * contain items that are not contained by this group.
	 */
	private final List<StructureItem> structure;
		
	public static DefaultGroupLayer newInstance(String groupId, List<AbstractDatasetLayer> datasets, List<PartialGroupLayer> groups, 
		List<StructureItem> structure) {
		
		Map<String, PartialGroupLayer> groupsMap = toMap(groups, PartialGroupLayer::getId);
		if(groupsMap.containsKey(groupId)) {
			return new DefaultGroupLayer(groupsMap.get(groupId), toMap(datasets, Layer::getId), groupsMap, structure);
		} else {
			throw new IllegalArgumentException("groupId not in groups list: " + groupId);
		}
	}
	
	DefaultGroupLayer(PartialGroupLayer partialGroupLayer, List<AbstractDatasetLayer> datasets, List<PartialGroupLayer> groups, 
		List<StructureItem> structure) {
		this(partialGroupLayer, toMap(datasets, Layer::getId), toMap(groups, PartialGroupLayer::getId), structure);
	}
	
	private DefaultGroupLayer(PartialGroupLayer partialGroupLayer, Map<String, AbstractDatasetLayer> datasets, Map<String, PartialGroupLayer> groups, 
		List<StructureItem> structure) {		
		
		this.partialGroupLayer = partialGroupLayer;
		this.datasets = datasets;
		this.groups = groups;
		this.structure = structure;		
	}
	
	private static <T, U> Map<U, T> toMap(List<T> items, Function<T, U> key) {
		return items.stream()
			.collect(Collectors.toMap(
				item -> key.apply(item),
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
	
	private void writeObject(ObjectOutputStream stream) throws IOException {
		serializer.write(stream, this);
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		serializer.read(stream, this);
	}
	
	@Override
	public boolean isWmsOnly() {
		return getLayers().stream()
			.filter(layer -> layer.getLayer().isWmsOnly())
			.findAny()
			.isPresent();
	}

	@Override
	public String toString() {
		return "DefaultGroupLayer [partialGroupLayer=" + partialGroupLayer
				+ ", datasets=" + datasets + ", groups=" + groups
				+ ", structure=" + structure + "]";
	}

		
}
