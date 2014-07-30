package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;

public class GetDataset implements Serializable {

	private static final long serialVersionUID = 8918475811419460221L;
	
	private final String id;
	
	public GetDataset(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}	

	@Override
	public String toString() {
		return "GetDataset [id=" + id + "]";
	}
}
