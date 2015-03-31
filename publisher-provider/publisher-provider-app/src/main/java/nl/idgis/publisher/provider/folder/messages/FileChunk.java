package nl.idgis.publisher.provider.folder.messages;

import nl.idgis.publisher.stream.messages.Item;

public class FileChunk extends Item {

	private static final long serialVersionUID = -1131141583719143799L;
	
	private final byte[] content;
	
	public FileChunk(byte[] content) {
		this.content = content;
	}

	public byte[] getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "FileChunk [content.length=" + content.length + "]";
	}
}
