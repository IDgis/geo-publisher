package nl.idgis.publisher.provider;

import akka.actor.Props;

public interface DatasetInfoSourceDesc {

	Props getProps();
	
	Class<?> getType();
	
	Object getRequest(String identification);
}
