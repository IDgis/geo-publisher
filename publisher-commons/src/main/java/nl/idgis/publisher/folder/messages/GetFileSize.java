package nl.idgis.publisher.folder.messages;

import java.io.Serializable;
import java.nio.file.Path;

public class GetFileSize implements Serializable {

	private static final long serialVersionUID = -2531444142947204029L;
	
	private final Path file;
	
	public GetFileSize(Path file) {
		this.file = file;
	}

	public Path getFile() {
		return file;
	}

	@Override
	public String toString() {
		return "GetFileSize [file=" + file + "]";
	}
}
