package nl.idgis.publisher.folder.messages;

import java.io.Serializable;
import java.nio.file.Path;

public class GetFileReceiver implements Serializable {
	
	private static final long serialVersionUID = -6655068221367757048L;
	
	private final Path file;

	public GetFileReceiver(Path file) {
		this.file = file;
	}

	public Path getFile() {
		return file;
	}

	@Override
	public String toString() {
		return "GetFileReceiver [file=" + file + "]";
	}
}
