package nl.idgis.publisher.service.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mysema.query.Tuple;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import nl.idgis.publisher.domain.web.tree.DefaultStyleRef;
import nl.idgis.publisher.domain.web.tree.StructureItem;
import nl.idgis.publisher.domain.web.tree.StyleRef;

class StructureProcessor {
	
	private final StringPath styleIdentificationPath;
	
	private final StringPath styleNamePath;
	
	private final StringPath childLayerIdentificationPath;
	
	private final StringPath parentLayerIdentificationPath;
	
	private final NumberPath<Integer> layerOrderPath;
	
	private final BooleanPath cyclePath;
	
	interface Result {
		List<StructureItem> getStructureItems();
		
		Map<String, Map<String, List<Optional<StyleRef>>>> getStyles();
	}
	
	StructureProcessor(
		StringPath styleIdentificationPath, 
		StringPath styleNamePath,
		StringPath childLayerIdentificationPath,
		StringPath parentLayerIdentificationPath,
		NumberPath<Integer> layerOrderPath,
		BooleanPath cyclePath) {
		
		this.styleIdentificationPath = styleIdentificationPath;
		this.styleNamePath = styleNamePath;
		this.childLayerIdentificationPath = childLayerIdentificationPath;
		this.parentLayerIdentificationPath = parentLayerIdentificationPath;
		this.layerOrderPath = layerOrderPath;
		this.cyclePath = cyclePath;
	}

	Result transform(List<Tuple> tuples) throws CycleException {
		List<StructureItem> structureItems = new ArrayList<>();
		
		Map<String, Map<String, List<Optional<StyleRef>>>> styles = new HashMap<>();
		
		String lastParentId = null;
		Integer lastLayerOrder = null;
		for(Tuple structureTuple : tuples) {
			String styleId = structureTuple.get(styleIdentificationPath);
			String styleName = structureTuple.get(styleNamePath);
			String childId = structureTuple.get(childLayerIdentificationPath);						
			String parentId = structureTuple.get(parentLayerIdentificationPath);
			Integer layerOrder = structureTuple.get(layerOrderPath);
				
			if(structureTuple.get(cyclePath)) {
				throw new CycleException(childId);
			}
			
			// skip duplicates
			if(!parentId.equals(lastParentId) || !layerOrder.equals(lastLayerOrder)) {
				Optional<StyleRef> styleRef = 
					Optional.ofNullable(styleId)
						.map(nonNullStyleId -> 
							new DefaultStyleRef(nonNullStyleId, styleName));
				
				structureItems.add(new StructureItem(childId, parentId, styleRef));				
			}
			
			lastParentId = parentId;
			lastLayerOrder = layerOrder;
		}
		
		return new Result() {

			@Override
			public List<StructureItem> getStructureItems() {
				return structureItems;
			}

			@Override
			public Map<String, Map<String, List<Optional<StyleRef>>>> getStyles() {				
				return styles;
			}
			
		};
	}
}
