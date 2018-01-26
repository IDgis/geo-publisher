package nl.idgis.publisher.monitor;

public class WMSException extends Exception {	
	
	private static final long serialVersionUID = 347362050740853678L;
	
	private final int responseCode;
	
	public WMSException(int responseCode, String message) {
		super((message == null ? "" : message + " ") +  "HTTP status: " + responseCode);
		
		this.responseCode = responseCode;
	}

	public WMSException(int responseCode) {
		this(responseCode, null);
	}
	
	public int getResponseCode() {
		return responseCode;
	}
	
}
