package nl.idgis.publisher.provider;

import akka.actor.Props;
import akka.actor.UntypedActor;

public class Database extends UntypedActor {
	
	public Database() {

	}
	
	public static Props props() {
		return Props.create(Database.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);
	}
}
