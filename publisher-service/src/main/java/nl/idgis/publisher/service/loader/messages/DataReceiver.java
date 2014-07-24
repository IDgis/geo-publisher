package nl.idgis.publisher.service.loader.messages;

import akka.actor.Props;
import akka.actor.UntypedActor;

public class DataReceiver extends UntypedActor {
	
	public DataReceiver() {
		
	}
	
	public static Props props() {
		return Props.create(DataReceiver.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);
	}
}
