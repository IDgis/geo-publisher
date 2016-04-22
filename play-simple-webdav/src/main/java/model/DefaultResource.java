package model.dav;

public class DefaultResource implements Resource {
	
	private final String contentType;
	
	private final byte[] content;
	
	public DefaultResource(String contentType, byte[] content) {
		this.contentType = contentType;
		this.content = content;
	}

	@Override
	public String contentType() {		
		return contentType;
	}

	@Override
	public byte[] content() {
		return content;
	}

}