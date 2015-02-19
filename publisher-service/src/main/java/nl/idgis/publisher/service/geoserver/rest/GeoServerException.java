package nl.idgis.publisher.service.geoserver.rest;

public class GeoServerException extends Exception {
	
	private static final long serialVersionUID = 5682934555123479768L;
	
	private final Integer httpResponse;
	
	public GeoServerException(Throwable cause) {
		this(null, null, cause);
	}
	
	public GeoServerException(String path, Throwable cause) {
		this(path, null, cause);
	}
	
	public GeoServerException(String path, Integer httpResponse) {
		this(path, httpResponse, null);
	}
	
	private static String message(String path, Integer httpResponse, Throwable cause) {		
		StringBuilder message = new StringBuilder();
		
		if(httpResponse != null) {
			message.append("unexpected http response: ");
			message.append(httpResponse);
		} 
		
		if(path != null) {
			if(message.length() > 0) {
				message.append(" ");
			}
			
			message.append("path: ");
			message.append(path);
		}			
		
		return message.length() > 0 ? message.toString() : null;
	}
	
	public GeoServerException(String path, Integer httpResponse, Throwable cause) {
		super(message(path, httpResponse, cause), cause);
		
		this.httpResponse = httpResponse;
	}
	
	public Integer getHttpResponse() {
		return httpResponse;
	}
}
