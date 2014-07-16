package nl.idgis.publisher.protocol.database;

import java.io.Serializable;

public class UnsupportedType implements Serializable {

	private static final long serialVersionUID = 6609325913721056449L;
	
	private final String className;
	
	public UnsupportedType(String className) {
		this.className = className;
	}

	@Override
	public String toString() {
		return "UnsupportedValue [className=" + className + "]";
	}	
	
}
