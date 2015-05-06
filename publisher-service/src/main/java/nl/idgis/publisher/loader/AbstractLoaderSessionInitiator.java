package nl.idgis.publisher.loader;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.sources.messages.FetchDataset;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.loader.messages.Busy;
import nl.idgis.publisher.loader.messages.SessionStarted;
import nl.idgis.publisher.protocol.messages.Ack;

public abstract class AbstractLoaderSessionInitiator<T extends ImportJobInfo> extends UntypedActor {
	
	protected static final Duration DEFAULT_RECEIVE_TIMEOUT = Duration.apply(15, TimeUnit.SECONDS);
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final T importJob;
	
	protected final ActorRef jobContext;
	
	private final Duration receiveTimeout;
	
	private boolean acknowledged = false;
	
	private ActorRef dataSource;
	
	protected AbstractLoaderSessionInitiator(T importJob, ActorRef jobContext, Duration receiveTimeout) {
		this.importJob = importJob;
		this.jobContext = jobContext;
		this.receiveTimeout = receiveTimeout;
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(receiveTimeout);
	}

	protected void become(String message, Procedure<Object> behavior) {
		log.debug("become: {}", message);
		
		getContext().become(new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof ReceiveTimeout) {
					log.debug("receive timeout");
					
					if(!acknowledged) {
						acknowledgeJob();
					}
					
					jobContext.tell(new UpdateJobState(JobState.ABORTED), getSelf());
					
					getContext().stop(getSelf());
				} else {
					behavior.apply(msg);
				}
				
			}
		});
	}
	
	protected void acknowledgeJobAndStop() {
		acknowledgeJob();
		getContext().stop(getSelf());
	}

	protected void acknowledgeJob() {
		acknowledged = true;
		jobContext.tell(new Ack(), getSelf());
	}
	
	protected abstract void dataSourceReceived() throws Exception;
	
	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof NotConnected) {					
			log.warning("not connected: " + importJob.getDataSourceId());
			
			acknowledgeJobAndStop();
		} else if(msg instanceof Busy) {
			log.debug("busy: " + importJob.getDataSourceId());
			
			acknowledgeJobAndStop();
		} else if(msg instanceof ActorRef) {
			log.debug("dataSource received: {}", msg);
			
			dataSource = (ActorRef)msg;
			
			dataSourceReceived();
		}
	}
	
	protected Procedure<Object> waitingForSessionStarted() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("session started");
					
					getContext().parent().tell(new SessionStarted(importJob, getSender()), getSelf());
					become("registering session start", waitingForSessionStartedAck());
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private Procedure<Object> waitingForSessionStartedAck() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("session started ack");
					
					acknowledgeJobAndStop();
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	protected void startLoaderSession(FetchDataset fetchDataset) throws Exception {
		dataSource.tell(fetchDataset, getSelf());
		
		become("starting session", waitingForSessionStarted());
	}
}
