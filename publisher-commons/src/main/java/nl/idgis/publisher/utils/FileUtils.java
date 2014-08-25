package nl.idgis.publisher.utils;

import java.io.File;

public class FileUtils {
	
	private FileUtils() {
		
	}

	public static void delete(File f) {
		if(f.isDirectory()) {
			for(File child : f.listFiles()) {
				delete(child);
			}
			
			f.delete();
		} else {			
			f.delete();
		}
	}
}
