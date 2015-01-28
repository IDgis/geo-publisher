package nl.idgis.publisher.job.context;

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
	public void postStop() throws Exception {
		log.debug("stopped");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.error("timeout while starting job");
			
			getContext().parent().tell(new Ack(), getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof Ack) {
			log.debug("acknowledged");
			
			getContext().parent().tell(msg, getSender());
			getContext().stop(getSelf());
		} else {
			log.debug("other message");
			
			getContext().become(started());
			getSelf().forward(msg, getContext());
		}
	}
	
	private Procedure<Object> finished() {
		log.debug("behavior: finished");
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("acknowledged");
					
					getContext().parent().tell(msg, getSelf());
					
					getContext().stop(getSelf());
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private Procedure<Object> acknowledged() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Log) {
					handleLog((Log)msg);
				} else if(msg instanceof AddJobNotification) {
					handleAddJobNotification((AddJobNotification)msg);
				} else if(msg instanceof RemoveJobNotification) {			
					handleRemoveJobNotification((RemoveJobNotification)msg);
				} else if(msg instanceof UpdateJobState) {
					log.debug("update job state received: {}", msg);
					
					JobState jobState = ((UpdateJobState)msg).getState();
					jobManager.tell(new UpdateState(jobInfo, jobState), getSender());
					
					if(jobState.isFinished()) {
						getContext().stop(getSelf());
					}
				} else {
					unhandled(msg);
				}
			}			
		};
	}
	
	private void handleRemoveJobNotification(RemoveJobNotification msg) {
		log.debug("remove notification received: {}", msg);
		
		jobManager.tell(new RemoveNotification(jobInfo, msg.getNotificationType()), getSender());
	}

	private void handleAddJobNotification(AddJobNotification msg) {
		log.debug("add notification received: {}", msg);
		
		jobManager.tell(new AddNotification(jobInfo, ((AddJobNotification)msg).getNotificationType()), getSender());
	}

	private void handleLog(Log msg) {
		log.debug("log received: {}", msg);
		
		jobManager.tell(new StoreLog(jobInfo, msg), getSender());
	}
	
	private Procedure<Object> started() {
		log.debug("behavior: started");
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Log) {
					handleLog((Log)msg);	
				} else if(msg instanceof AddJobNotification) {
					handleAddJobNotification((AddJobNotification)msg);
				} else if(msg instanceof RemoveJobNotification) {
					handleRemoveJobNotification((RemoveJobNotification)msg);					
				} if(msg instanceof Ack) {
					log.debug("acknowledged");
					
					getContext().parent().tell(msg, getSelf());
					
					getContext().become(acknowledged());
				} else if(msg instanceof UpdateJobState) {
					log.debug("update job state received: {}", msg);
					
					JobState jobState = ((UpdateJobState)msg).getState();
					jobManager.tell(new UpdateState(jobInfo, jobState), getSender());
					
					if(jobState.isFinished()) {
						getContext().become(finished());
					}
				} else { 
					unhandled(msg);
				}				
			}
			
		};
	}
}
