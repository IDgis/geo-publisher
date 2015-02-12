package nl.idgis.publisher.service.geoserver.rest;

public class GeoServerException extends Exception {
	
	private static final long serialVersionUID = 5682934555123479768L;
	
	private final Integer httpResponse;

	public GeoServerException() {
		this(null, null);
	}
	
	public GeoServerException(Integer httpResponse) {
		this(httpResponse, null);
	}
	
	public GeoServerException(Throwable cause) {
		this(null, cause);		
	}
	
	private static String message(Integer httpResponse, Throwable cause) {
		if(httpResponse != null) {
			return "unexpected http response: " + httpResponse;
		}
		
		return null;
	}
	
	public GeoServerException(Integer httpResponse, Throwable cause) {
		super(message(httpResponse, cause), cause);
		
		this.httpResponse = httpResponse;
	}
	
	public Integer getHttpResponse() {
		return httpResponse;
	}
}
