package nl.idgis.publisher.service.manager.messages;

public interface Layer {
	
	String getId();

	boolean isGroup();
	GroupLayer asGroup();
	
	boolean isDataset();
	DatasetLayer asDataset();
}
