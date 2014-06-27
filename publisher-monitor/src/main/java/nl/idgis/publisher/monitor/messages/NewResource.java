package nl.idgis.publisher.monitor.messages;

import java.io.Serializable;

public class NewResource extends ResourceRef implements Serializable {
	
	private static final long serialVersionUID = 2067751754589850592L;

	public NewResource(Object resourceType, Object resource) {
		super(resourceType, resource);		
	}
	
}
