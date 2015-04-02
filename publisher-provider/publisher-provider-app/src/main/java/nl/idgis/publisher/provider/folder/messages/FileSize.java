package nl.idgis.publisher.provider.folder.messages;

import java.io.Serializable;

public class FileSize implements Serializable {

	private static final long serialVersionUID = -6970038605542196398L;
	
	private final long size;
	
	public FileSize(long size) {
		this.size = size;
	}
	
	public long getSize() {
		return size;
	}

	@Override
	public String toString() {
		return "FileSize [size=" + size + "]";
	}
}
