package nl.idgis.publisher.domain.web.tree;

import java.util.List;

public interface DatasetLayer extends Layer {
	
	String getDatasetId();
	
	List<String> getKeywords();
	
	List<StyleRef> getStyleRefs();
	
	boolean isVectorLayer();
	
	VectorDatasetLayer asVectorLayer();
	
	boolean isRasterLayer();
	
	RasterDatasetLayer asRasterLayer();
}
