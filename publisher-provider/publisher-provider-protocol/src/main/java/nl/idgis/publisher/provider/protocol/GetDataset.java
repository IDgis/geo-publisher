package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

public class GetDataset implements Serializable {

	private static final long serialVersionUID = -8635139807073207401L;
	
	private final String id;
	
	public GetDataset(String id) {
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}

	@Override
	public String toString() {
		return "GetDataset [id=" + id + "]";
	}
}
