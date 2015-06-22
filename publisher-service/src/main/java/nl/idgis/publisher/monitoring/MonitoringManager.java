package nl.idgis.publisher.monitoring;

import com.ning.http.client.AsyncHttpClient;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.monitoring.tester.ServiceTester;
import nl.idgis.publisher.monitoring.tester.messages.StatusReport;

public class MonitoringManager extends UntypedActor {
	
	private final AsyncHttpClient asyncHttpClient;
	
	private final ActorRef serviceManager;
	
	private ActorRef serviceTester;
	
	public MonitoringManager(AsyncHttpClient asyncHttpClient, ActorRef serviceManager) {
		this.asyncHttpClient = asyncHttpClient;
		this.serviceManager = serviceManager;
	}
	
	public static Props props(AsyncHttpClient asyncHttpClient, ActorRef serviceManager) {
		return Props.create(MonitoringManager.class, asyncHttpClient, serviceManager);
	}
	
	public final void preStart() {
		serviceTester = getContext().actorOf(ServiceTester.props(asyncHttpClient, getSelf()));
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof StatusReport) {
			
		} else {
			unhandled(msg);
		}
	}

}
