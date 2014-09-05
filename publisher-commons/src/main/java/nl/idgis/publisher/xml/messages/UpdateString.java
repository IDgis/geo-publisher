package nl.idgis.publisher.xml.messages;

import com.google.common.collect.BiMap;

public class UpdateString extends Query<Void> {
	
	private static final long serialVersionUID = 4775616027495444969L;
	
	private final String newValue;

	public UpdateString(BiMap<String, String> namespaces, String path, String newValue) {
		super(namespaces, path);
		
		this.newValue = newValue;
	}
	
	public String getNewValue() {
		return this.newValue;
	}

	@Override
	public String toString() {
		return "UpdateString [newValue=" + newValue + "]";
	}

}
