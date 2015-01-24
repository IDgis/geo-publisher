package nl.idgis.publisher.job;

import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.job.context.JobContext;
import nl.idgis.publisher.protocol.messages.Ack;

public class JobExecutorFacade extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef jobManager, target;
	
	private Map<ActorRef, ActorRef> senders;

	public JobExecutorFacade(ActorRef jobManager, ActorRef target) {
		this.jobManager = jobManager;
		this.target = target;
	}
	
	public static Props props(ActorRef jobManager, ActorRef target) {
		return Props.create(JobExecutorFacade.class, jobManager, target);
	}
	
	@Override
	public void preStart() {
		senders = new HashMap<ActorRef, ActorRef>();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof JobInfo) {
			log.debug("job info received");
			
			ActorRef jobContext = getContext().actorOf(JobContext.props(jobManager, (JobInfo)msg));
			senders.put(jobContext, getSender());
			target.tell(msg, jobContext);
		} else if(msg instanceof Ack) {
			log.debug("ack received");
			
			ActorRef jobContext = senders.get(getSender());
			if(jobContext != null) {
				jobContext.tell(msg, getSelf());
			} else {
				target.forward(msg, getContext());
			}
		} else {
			target.forward(msg, getContext());
		}
	}
	
}