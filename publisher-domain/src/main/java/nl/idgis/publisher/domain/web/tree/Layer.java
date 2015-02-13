package nl.idgis.publisher.domain.web.tree;


public interface Layer extends Item {
	
	boolean isGroup();
	
	GroupLayer asGroup();
	
	DatasetLayer asDataset();
}
