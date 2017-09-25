package nl.idgis.publisher.folder.messages;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Date;

public class FetchEntireFile implements Serializable {
	
	private static final long serialVersionUID = 3967685012468844197L;
	
	private final Date metadataRevisionDate;
	private final Path file;
	private final String physicalName;
	
	public FetchEntireFile(Date metadataRevisionDate, Path file, String physicalName) {
		this.metadataRevisionDate = metadataRevisionDate;
		this.file = file;
		this.physicalName = physicalName;
	}
	
	public Date getMetadataRevisionDate() {
		return metadataRevisionDate;
	}
	
	public Path getFile() {
		return file;
	}
	
	public String getPhysicalName() {
		return physicalName;
	}

	@Override
	public String toString() {
		return "FetchEntireFile [metadataRevisionDate=" + metadataRevisionDate + ", file=" + 
				file + ", physicalName=" + physicalName + "]";
	}
}
