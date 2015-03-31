package nl.idgis.publisher.provider.folder.messages;

import java.io.Serializable;
import java.nio.file.Path;

public class FetchFile implements Serializable {	

	private static final long serialVersionUID = -6350085861872222898L;
	
	private final Path file;
	
	public FetchFile(Path file) {
		this.file = file;
	}

	public Path getFile() {
		return file;
	}

	@Override
	public String toString() {
		return "FetchFile [file=" + file + "]";
	}
}
