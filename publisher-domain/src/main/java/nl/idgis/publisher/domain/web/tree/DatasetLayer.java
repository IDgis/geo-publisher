package nl.idgis.publisher.domain.web.tree;

import java.util.List;
import java.util.Optional;

public interface DatasetLayer extends Layer {
	
	Optional<String> getMetadataFileIdentification ();
	
	List<String> getKeywords();
	
	List<StyleRef> getStyleRefs();
	
	List<String> getUserGroups();
	
	boolean isVectorLayer();
	
	VectorDatasetLayer asVectorLayer();
	
	boolean isRasterLayer();
	
	RasterDatasetLayer asRasterLayer();
}
