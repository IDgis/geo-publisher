package nl.idgis.publisher.provider.metadata.messages;

import java.io.Serializable;
import java.util.Map;

public class StoreFileNames implements Serializable {

	private static final long serialVersionUID = -6290130853632359065L;
	
	private final Map<String, String> fileNames;
	
	public StoreFileNames(Map<String, String> fileNames) {
		this.fileNames = fileNames;
	}

	public Map<String, String> getFileNames() {
		return fileNames;
	}

	@Override
	public String toString() {
		return "StoreFileNames [fileNames=" + fileNames + "]";
	}
}
