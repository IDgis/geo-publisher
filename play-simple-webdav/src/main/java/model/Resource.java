package model.dav;

public interface Resource {	
	
	String contentType();
	
	byte[] content();
}