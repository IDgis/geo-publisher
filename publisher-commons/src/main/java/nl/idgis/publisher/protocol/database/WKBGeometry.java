package nl.idgis.publisher.protocol.database;

import java.io.Serializable;
import java.util.Arrays;

import com.google.common.io.BaseEncoding;

public class WKBGeometry implements Serializable {
	
	private static final long serialVersionUID = 810212567904962661L;
	
	public final byte[] bytes;
	
	public WKBGeometry(byte[] bytes) {
		this.bytes = bytes;
	}
	
	public byte[] getBytes() {
		return Arrays.copyOf(bytes, bytes.length);
	}

	@Override
	public String toString() {
		return "WKBGeometry [bytes=" + BaseEncoding.base16().lowerCase().encode(bytes) + "]";
	}
}
