package nl.idgis.publisher.xml.messages;

import java.io.Serializable;
import java.util.Arrays;

public class ParseDocument implements Serializable {
	
	private static final long serialVersionUID = 7543103868047290157L;
	
	private final byte[] content;
	
	public ParseDocument(byte[] content) {
		this.content = content;
	}
	
	public byte[] getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "ParseDocument [content=" + Arrays.toString(content) + "]";
	}
}
