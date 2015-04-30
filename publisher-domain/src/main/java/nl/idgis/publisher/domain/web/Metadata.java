package nl.idgis.publisher.domain.web;

import java.util.Arrays;

public class Metadata extends Identifiable {

	private static final long serialVersionUID = -6552191480250828698L;
	
	private final byte[] content;
	
	public Metadata(String id, byte[] content) {
		super(id);
		
		this.content = content;
	}
	
	public byte[] content() {
		return content;
	}

	@Override
	public String toString() {
		return "Metadata [content=" + Arrays.toString(content) + "]";
	}
}
