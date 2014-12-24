package nl.idgis.publisher.dataset;

import akka.actor.UntypedActor;

public class DatasetManager extends UntypedActor {

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);
	}

}
