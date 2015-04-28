package nl.idgis.publisher.domain.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class FileLog extends DatasetLog<FileLog> {

	private static final long serialVersionUID = 5783288525218984045L;
	
	private final String fileName;

	@JsonCreator
	public FileLog(
			@JsonProperty("fileName") String fileName) {
		
		this.fileName = fileName;
	}
	
	private FileLog(Dataset dataset, String fileName) {
		super(dataset);
		
		this.fileName = fileName;
	}
	
	public String getFileName() {
		return this.fileName;
	}
	
	@Override
	public FileLog withDataset(Dataset dataset) {
		return new FileLog(dataset, fileName);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileLog other = (FileLog) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FileLog [fileName=" + fileName + "]";
	}
}
