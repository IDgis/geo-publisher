package nl.idgis.publisher.monitor;

public class WMSException extends Exception {	
	
	private static final long serialVersionUID = -2923377660306258566L;
	
	private final int responseCode;

	public WMSException(int responseCode) {
		super("unexpected response code: " + responseCode);
		
		this.responseCode = responseCode;
	}
	
	public int getResponseCode() {
		return responseCode;
	}
	
}
