package nl.idgis.publisher.monitor;

import nl.idgis.publisher.monitor.messages.NotActive;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class InactiveMonitor extends UntypedActor {

	public InactiveMonitor() {
		
	}
	
	public static Props props() {
		return Props.create(InactiveMonitor.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		getSender().tell(new NotActive(), getSelf());
	}
}
