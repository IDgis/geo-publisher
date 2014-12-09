package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;
import java.util.Arrays;

import com.google.common.io.BaseEncoding;

/**
 * Container for a well known binary (WKB) geometry.
 * 
 * @author copierrj
 *
 */
public class WKBGeometry implements Serializable {
	
	private static final long serialVersionUID = 810212567904962661L;
	
	public final byte[] bytes;
	
	/**
	 * Creates a well known binary geometry container object.
	 * @param bytes the binary value.
	 */
	public WKBGeometry(byte[] bytes) {
		this.bytes = bytes;
	}
	
	/**
	 * 
	 * @return the binary value
	 */
	public byte[] getBytes() {
		return Arrays.copyOf(bytes, bytes.length);
	}

	@Override
	public String toString() {
		return "WKBGeometry [bytes=" + BaseEncoding.base16().lowerCase().encode(bytes) + "]";
	}
}
