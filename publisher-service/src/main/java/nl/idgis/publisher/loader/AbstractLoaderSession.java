package nl.idgis.publisher.loader;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.loader.messages.SessionFinished;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.utils.FutureUtils;

public abstract class AbstractLoaderSession<T extends ImportJobInfo, U extends StartImport> extends UntypedActor {
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	protected static final FiniteDuration DEFAULT_RECEIVE_TIMEOUT = Duration.apply(30, TimeUnit.SECONDS);
	
	protected final Duration receiveTimeout;
	
	private final ActorRef loader;
	
	protected final ActorRef jobContext;
	
	protected final T importJob;
	
	protected FutureUtils f;
	
	protected long progressTarget = 0;
	
	protected static class FinalizeSession implements Serializable {

		private static final long serialVersionUID = -6298981994732740388L;
		
		private final JobState jobState;
		
		FinalizeSession(JobState jobState) {
			this.jobState = jobState;
		}
		
		JobState getJobState() {
			return this.jobState;
		}

		@Override
		public String toString() {
			return "FinalizeSession [jobState=" + jobState + "]";
		}		
	}
	
	protected AbstractLoaderSession(Duration receiveTimeout, ActorRef loader, T importJob, ActorRef jobContext) {
		this.receiveTimeout = receiveTimeout;
		this.loader = loader;
		this.importJob = importJob;
		this.jobContext = jobContext;
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(receiveTimeout);
		
		f = new FutureUtils(getContext());
	}
	
	private void handleTimeout() {
		log.error("timeout while executing job: {}", importJob);
		
		getSelf().tell(new FinalizeSession(JobState.ABORTED), getSelf());
	}
	
	protected void onReceiveElse(Object msg) {
		unhandled(msg);
	}
	
	protected final void onReceiveCommon(Object msg) {
		if(msg instanceof ReceiveTimeout) {				
			handleTimeout();
		} else if(msg instanceof FinalizeSession) {
			handleFinalizeSession((FinalizeSession)msg);
		} else if(msg instanceof Failure) {
			handleFailure((Failure)msg);
		} else if(msg instanceof End) {
			handleEnd((End)msg);
		} else {
			onReceiveElse(msg);
		}
	}
	
	protected void handleFinalizeSession(FinalizeSession finalizeSession) {
		JobState state = finalizeSession.getJobState();
		
		log.debug("finalizing session: {}",  state);
		
		ActorRef self = getSelf();
		f.ask(jobContext, new UpdateJobState(state)).whenComplete((msg0, t0) -> {
			if(t0 != null) {
				log.error("couldn't change job state: {}", t0);
			} 
			
			f.ask(loader, new SessionFinished(importJob)).whenComplete((msg1, t1) -> {
				if(t1 != null) {
					log.error("couldn't finish import session: {}", t1);
				}
				
				log.debug("session finalized");
				
				self.tell(PoisonPill.getInstance(), self);
			});
		});
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof StartImport) {
			handleStartImport((U)msg);
		} else {
			onReceiveCommon(msg);
		}
	}
	
	protected abstract long progress();
	
	protected abstract long progressTarget(U startImport);
	
	protected void updateProgress() {
		loader.tell(new Progress(progress(), progressTarget), getSelf());
	}
	
	private Procedure<Object> importing() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Item<?>) {
					handleItem(((Item<?>)msg).getContent());
				} else {
					onReceiveCommon(msg);
				}
			}			
		};
	}
	
	protected abstract void handleItem(Object content) throws Exception;
	
	private void handleStartImport(U msg) {
		log.info("starting import");
		
		progressTarget = progressTarget(msg);
		
		loader.tell(new Progress(0, progressTarget), getSelf());
		
		// data source
		getSender().tell(new Ack(), getSelf());
		
		// session initiator
		msg.getInitiator().tell(new Ack(), getSelf());
				
		getContext().become(importing());
	}
	
	protected CompletableFuture<Object> importFailed() {
		return f.successful(null);
	}
	
	private void handleFailure(final Failure failure) {
		log.error("import failed: {}", failure.getCause());
		
		ActorRef self = getSelf();
		importFailed().thenApply(msg -> {
			log.debug("finalizing session (failed)");
			
			return new FinalizeSession(JobState.FAILED);
		}).exceptionally(t -> {
			log.error("couldn't properly finalize session: {}", t);
			
			return new FinalizeSession(JobState.FAILED);
		}).thenAccept(msg -> {
			self.tell(msg, self);
		});	
	}
	
	protected CompletableFuture<Object> importSucceeded() {
		return f.successful(null);
	}
	
	private void handleEnd(final End end) {
		log.info("import completed");
		
		ActorRef self = getSelf();
						
		importSucceeded().thenApply(msg -> {
			log.debug("finalizing session (succeeded)");
			
			return new FinalizeSession(JobState.SUCCEEDED);
		}).exceptionally(t -> {
			log.error("couldn't properly finalize session: {}", t);
			
			return new FinalizeSession(JobState.FAILED);
		}).thenAccept(msg -> {
			self.tell(msg, self);
		});
	}
}
