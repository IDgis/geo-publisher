package nl.idgis.publisher.service.manager.messages;

public interface Layer extends Item {
	
	boolean isGroup();
	GroupLayer asGroup();
	
	boolean isDataset();
	DatasetLayer asDataset();
}
