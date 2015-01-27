package nl.idgis.publisher.job.context;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.job.context.messages.AddJobNotification;
import nl.idgis.publisher.job.context.messages.RemoveJobNotification;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.AddNotification;
import nl.idgis.publisher.job.manager.messages.RemoveNotification;
import nl.idgis.publisher.job.manager.messages.StoreLog;
import nl.idgis.publisher.job.manager.messages.UpdateState;
import nl.idgis.publisher.protocol.messages.Ack;

public class JobContext extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef jobManager;
	
	private final JobInfo jobInfo;
	
	private static class JobFinished implements Serializable {
		
		private static final long serialVersionUID = 2236542274270022155L;		
	}
	
	private static class JobInProgress implements Serializable {
		
		private static final long serialVersionUID = -5282262057445760600L;		
	}
	
	private static class JobAcknowledged implements Serializable {
		
		private static final long serialVersionUID = -3851776170365172160L;		
	}
		
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

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.error("timeout while starting job");
			
			getContext().parent().tell(new Ack(), getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof JobAcknowledged) {
			log.debug("job acknowledged");
			
			getContext().stop(getSelf());
		} else if(msg instanceof JobInProgress) {
			log.debug("job started");
			
			getContext().become(started());
		} else {
			onReceiveElse(msg);
		}
	}
	
	private Procedure<Object> started() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof JobFinished) {					
					log.debug("job acknowledged -> stop context");
					
					getContext().stop(getSelf());					
				} else {
					onReceiveElse(msg);
				}				
			}
			
		};
	}
	
	public void onReceiveElse(Object msg) {
		if(msg instanceof Ack) {
			getContext().parent().tell(msg, getSelf());
			
			getSelf().tell(new JobAcknowledged(), getSelf());
		} else if(msg instanceof UpdateJobState) {
			log.debug("update job state received: {}", msg);
			
			JobState jobState = ((UpdateJobState)msg).getState();
			jobManager.tell(new UpdateState(jobInfo, jobState), getSender());
			
			if(jobState.isFinished()) {
				getSelf().tell(new JobFinished(), getSelf());
			} else {
				getSelf().tell(new JobInProgress(), getSelf());
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
