package nl.idgis.publisher.job.context;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.messages.AddNotification;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.RemoveNotification;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.database.messages.UpdateJobState;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.job.context.messages.AddJobNotification;
import nl.idgis.publisher.job.context.messages.RemoveJobNotification;
import nl.idgis.publisher.protocol.messages.Ack;

public class JobContext extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef jobManager;
	
	private final JobInfo jobInfo;
	
	private boolean stopOnAck;
	
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
		
		stopOnAck = true;
	}
	
	private void sendAck() {
		getContext().parent().tell(new Ack(), getSelf());
		
		if(stopOnAck) {
			log.debug("stopping on ack");
			
			//getContext().stop(getSelf());
		} else {
			log.debug("job in progress");
			
			getContext().setReceiveTimeout(Duration.create(5, TimeUnit.MINUTES));
		}
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.debug("timeout");
			
			sendAck();
		} else if(msg instanceof Ack) {
			log.debug("ack received");
			
			sendAck();
		} else if(msg instanceof JobState) {
			log.debug("job state received: {}", msg);
			
			stopOnAck = false;
			
			JobState jobState = (JobState)msg;
			jobManager.tell(new UpdateJobState(jobInfo, jobState), getSender());
			
			if(jobState.isFinished()) {
				log.debug("job finished");
				
				//getContext().stop(getSelf());
			}
		} else if(msg instanceof Log) {
			log.debug("log received: {}", msg);
			
			jobManager.tell(new StoreLog(jobInfo, (Log)msg), getSender());
		} else if(msg instanceof AddJobNotification) {
			log.debug("add notification received: {}", msg);
			
			jobManager.tell(new AddNotification(jobInfo, ((AddJobNotification)msg).getNotificationType()), getSender());
		} else if(msg instanceof RemoveJobNotification) {			
			log.debug("remove notification received: {}", msg);
			
			jobManager.tell(new RemoveNotification(jobInfo, ((RemoveJobNotification)msg).getNotificationType()), getSender());
		} else {
			unhandled(msg);
		}
	}
}
