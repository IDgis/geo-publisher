package nl.idgis.publisher.monitoring;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.monitoring.tester.ServiceTester;
import nl.idgis.publisher.monitoring.tester.messages.StatusReport;

public class MonitoringManager extends UntypedActor {
	
	private final ActorRef serviceManager;
	
	private ActorRef serviceTester;
	
	public MonitoringManager(ActorRef serviceManager) {
		this.serviceManager = serviceManager;
	}
	
	public static Props props(ActorRef serviceManager) {
		return Props.create(MonitoringManager.class, serviceManager);
	}
	
	public final void preStart() {
		serviceTester = getContext().actorOf(ServiceTester.props(getSelf()));
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof StatusReport) {
			
		} else {
			unhandled(msg);
		}
	}

}
