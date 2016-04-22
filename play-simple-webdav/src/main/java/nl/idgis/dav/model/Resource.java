package nl.idgis.dav.model;

public interface Resource {	
	
	String contentType();
	
	byte[] content();
}