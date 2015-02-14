package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class DoQuery implements Serializable {

	private static final long serialVersionUID = 1047615240061818253L;
	
	private final Class<?> clazz;
	
	public DoQuery(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	public Class<?> getClazz() {
		return clazz;
	}

	@Override
	public String toString() {
		return "DoQuery [clazz=" + clazz + "]";
	}
}
