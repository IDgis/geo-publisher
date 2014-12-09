package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

/**
 * Placeholder value for an unsupported data type. 
 * 
 * @author copierrj
 *
 */
public class UnsupportedType implements Serializable {

	private static final long serialVersionUID = 6609325913721056449L;
	
	private final String className;
	
	/**
	 * Creates a place holder for an unsupported data type.
	 * 
	 * @param className the original className of the value.
	 */
	public UnsupportedType(String className) {
		this.className = className;
	}

	@Override
	public String toString() {
		return "UnsupportedValue [className=" + className + "]";
	}	
	
}
