package nl.idgis.publisher.provider.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;

public class MetadataParser {
	
	private MetadataParser() {
		
	}
	
	public static MetadataItem createMetadataItem(File file) throws IOException {
		String fileName = file.getName();
		String id = fileName.substring(0, fileName.lastIndexOf("."));
		
		FileInputStream fis = new FileInputStream(file); 
		byte[] content = IOUtils.toByteArray(fis);
		fis.close();		
		
		return new MetadataItem(id, content);
	}
}
