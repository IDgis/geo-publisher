package nl.idgis.publisher.xml.exceptions;

import java.io.Serializable;

public class NotParseable extends Exception implements Serializable {	
	
	private static final long serialVersionUID = -4962047163990747282L;
	
	public NotParseable(Throwable cause) {
		super(cause);
	}	
	
}
