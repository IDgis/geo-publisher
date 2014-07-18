package nl.idgis.publisher.domain;

import java.io.Serializable;

public interface StatusType extends Serializable {

	StatusCategory statusCategory ();
	
	public static enum StatusCategory {
		INFO,
		SUCCESS,
		WARNING,
		ERROR
	}
}
