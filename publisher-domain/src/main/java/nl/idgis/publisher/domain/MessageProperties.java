package nl.idgis.publisher.domain;

import java.io.Serializable;

public interface MessageProperties extends Serializable {

	 EntityType getEntityType ();
	 
	 String getIdentification ();
	 
	 String getTitle ();
	 
	 StatusType getStatus ();
}
