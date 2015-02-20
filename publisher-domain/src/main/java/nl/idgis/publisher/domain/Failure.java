package nl.idgis.publisher.domain;

import java.io.Serializable;

public class Failure implements Serializable {

	private static final long serialVersionUID = 5767771486295031269L;
	
	private final String exceptionId;
	
	public Failure(String exceptionId) {
		this.exceptionId = exceptionId;
	}
	
	public String getExceptionId() {
		return exceptionId;
	}

	@Override
	public String toString() {
		return "Failure [exceptionId=" + exceptionId + "]";
	}
}
