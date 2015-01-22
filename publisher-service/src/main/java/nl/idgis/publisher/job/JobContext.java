package nl.idgis.publisher.job;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.protocol.messages.Ack;

public class JobContext extends UntypedActor {
	
	private final ActorRef jobManager;
	
	private final JobInfo jobInfo;
	
	public JobContext(ActorRef jobManager, JobInfo jobInfo) {
		this.jobManager = jobManager;
		this.jobInfo = jobInfo;
	}
	
	public static Props props(ActorRef jobManager, JobInfo jobInfo) {
		return Props.create(JobContext.class, jobManager, jobInfo);
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(10, TimeUnit.SECONDS));
	}
	
	private void sendAck() {
		getContext().parent().tell(new Ack(), getSelf());
		getContext().stop(getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			sendAck();
		} else if(msg instanceof Ack) {
			sendAck();
		} else {	
			unhandled(msg);
		}
	}
}
