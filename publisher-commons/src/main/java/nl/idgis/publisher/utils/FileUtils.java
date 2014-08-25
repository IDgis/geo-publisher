package nl.idgis.publisher.utils;

import java.io.File;

public class FileUtils {
	
	private FileUtils() {
		
	}

	public static boolean delete(File f) {
		if(f.isDirectory()) {
			for(File child : f.listFiles()) {
				if(!delete(child)) {
					return false;
				}
			}
		
		}			
		
		return f.delete();	
	}
}
