package nl.idgis.publisher.metadata;

import akka.actor.Props;
import akka.actor.UntypedActor;

public class HarvesterMock extends UntypedActor {
	
	public static Props props() {
		return Props.create(HarvesterMock.class);
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {

		
	}

}
