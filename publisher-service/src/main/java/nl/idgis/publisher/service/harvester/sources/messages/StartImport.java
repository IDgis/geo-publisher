package nl.idgis.publisher.service.harvester.sources.messages;

import java.io.Serializable;

public class StartImport implements Serializable {
	
	private static final long serialVersionUID = -1320088517921643402L;
	
	private final long count;
	
	public StartImport(long count) {
		this.count = count;
	}
	
	public long getCount() {
		return count;
	}

	@Override
	public String toString() {
		return "StartImport [count=" + count + "]";
	}
}
