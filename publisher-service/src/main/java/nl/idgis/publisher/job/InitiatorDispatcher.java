package nl.idgis.publisher.job;

import java.util.Iterator;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.TypedIterable;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.database.messages.JobInfo;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class InitiatorDispatcher extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final FiniteDuration timeout;
	
	private final ActorRef jobManager, target;
	
	public InitiatorDispatcher(ActorRef jobManager, ActorRef target, FiniteDuration timeout) {
		this.jobManager = jobManager;
		this.target = target;
		this.timeout = timeout;
	}
	
	public static Props props(ActorRef jobManager, ActorRef target, FiniteDuration timeout) {
		return Props.create(InitiatorDispatcher.class, jobManager, target, timeout);
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(timeout);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof TypedIterable) {
			log.debug("typed iterable received: " + msg);
			
			TypedIterable<?> typedIterable = (TypedIterable<?>)msg;
			if(typedIterable.contains(JobInfo.class)) {
			
				final Iterator<JobInfo> i = typedIterable.cast(JobInfo.class).iterator();
				
				if(i.hasNext()) {
					final JobInfo jobInfo = (JobInfo)i.next();
					
					tellTarget(jobInfo);
					
					getContext().become(new Procedure<Object>() {
	
						@Override
						public void apply(Object msg) throws Exception {
							if(msg instanceof Ack) {
								log.debug("ack received");
								
								if(i.hasNext()) {
									tellTarget(i.next());
								} else {
									stop();
								}
							} else if(msg instanceof ReceiveTimeout) {
								log.error("timeout");
								
								stop();
							} else {
								log.debug("unhandled (waiting for Ack): " + msg);
								
								unhandled(msg);
							}
						}
						
					});					
				} else {
					stop();
				}
			} else {
				log.debug("unhandled (expected TypeIterable containing JobInfo");
				
				unhandled(msg);
			}
		} else {
			log.debug("unhandled (waiting for TypedIterable): " + msg);
			
			unhandled(msg);
		}
	}
	
	private void tellTarget(JobInfo jobInfo) {
		log.debug("sending message to target: " + jobInfo);
		
		ActorRef jobContext = getContext().actorOf(
				JobContext.props(jobManager, jobInfo), 
				nameGenerator.getName(JobContext.class));
		
		target.tell(jobInfo, jobContext);
	}	
	
	private void stop() {
		log.debug("stopping");
		
		getContext().parent().tell(new Ack(), getSelf());
		getContext().become(receive());
	}
}
