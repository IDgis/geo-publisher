package nl.idgis.publisher.service.provisioning;

import static org.junit.Assert.assertEquals;

import java.util.Set;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.EnsureServiceJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.service.provisioning.messages.AddPublicationService;
import nl.idgis.publisher.service.provisioning.messages.AddStagingService;
import nl.idgis.publisher.service.provisioning.messages.RemovePublicationService;
import nl.idgis.publisher.service.provisioning.messages.RemoveStagingService;
import nl.idgis.publisher.utils.FutureUtils;

public class ProvisioningManagerTest  {
	
	public abstract static class ServiceActorInfo {
		
		private final ServiceInfo serviceInfo;
		
		private final String schema;
	
		protected ServiceActorInfo(ServiceInfo serviceInfo, String schema) {
			this.serviceInfo = serviceInfo;
			this.schema = schema;
		}

		public ServiceInfo getServiceInfo() {
			return serviceInfo;
		}

		public String getSchema() {
			return schema;
		}
	}
	
	public static class ServiceActorStarted extends ServiceActorInfo {

		public ServiceActorStarted(ServiceInfo serviceInfo, String schema) {
			super(serviceInfo, schema);
		}
	}
	
	public static class ServiceActorStopped extends ServiceActorInfo {

		public ServiceActorStopped(ServiceInfo serviceInfo, String schema) {
			super(serviceInfo, schema);
		}
	}
	
	public static class ServiceActor extends UntypedActor {
		
		private final ActorRef recorder;
		
		private final ServiceInfo serviceInfo;
		
		private final String schema;
		
		public ServiceActor(ActorRef recorder, ServiceInfo serviceInfo, String schema) {
			this.recorder = recorder;
			this.serviceInfo = serviceInfo;
			this.schema = schema;
		}
		
		public static Props props(ActorRef recorder, ServiceInfo serviceInfo, String schema) {
			return Props.create(ServiceActor.class, recorder, serviceInfo, schema);
		}
		
		@Override
		public void preStart() throws Exception {
			recorder.tell(new ServiceActorStarted(serviceInfo, schema), getSelf());
		}
		
		@Override
		public void postStop() throws Exception {
			recorder.tell(new ServiceActorStopped(serviceInfo, schema), getSelf());
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			unhandled(msg);	
		}
	};
	
	public static class JobActorStarted {
		
		private final Set<ActorRef> targets;
		
		public JobActorStarted(Set<ActorRef> targets) {
			this.targets = targets;
		}
		
		public Set<ActorRef> getTargets() {
			return targets;
		}
	}
	
	public static class FinishJob {
		
	}
	
	public static class JobActor extends UntypedActor {
		
		private final ActorRef recorder;
		
		private final Set<ActorRef> targets;
		
		public JobActor(ActorRef recorder, Set<ActorRef> targets) {
			this.recorder = recorder;
			this.targets = targets;
		}
		
		public static Props props(ActorRef recorder, Set<ActorRef> targets) {
			return Props.create(JobActor.class, recorder, targets);
		}
		
		@Override
		public void preStart() throws Exception {
			recorder.tell(new JobActorStarted(targets), getSelf());
		}
		
		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof FinishJob) {
				targets.stream().forEach(target ->
					getContext().parent().tell(new UpdateJobState(JobState.SUCCEEDED), target));
				
				getContext().stop(getSelf());
			} else {
				unhandled(msg);
			}
		}
	};
	
	FutureUtils f;
	
	ActorSystem actorSystem;
	
	ActorRef recorder, provisioningManager;
	
	@Before
	public void start() {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		recorder = actorSystem.actorOf(AnyRecorder.props(), "recorder");
		provisioningManager = actorSystem.actorOf(
			ProvisioningManager.props(
				actorSystem.deadLetters(),
				actorSystem.deadLetters(),
				new ProvisioningPropsFactory() {
					
					@Override
					public Props serviceProps(ServiceInfo serviceInfo, String schema) {
						return ServiceActor.props(recorder, serviceInfo, schema);
					}
					
					@Override
					public Props ensureJobProps(Set<ActorRef> targets) {
						return JobActor.props(recorder, targets);
					}
				}));
		
		f = new FutureUtils(actorSystem);
	}
	
	@After
	public void stop() {
		actorSystem.shutdown();
	}
	
	@Test
	public void testServiceJobInfo() throws Exception {
		ServiceInfo stagingServiceInfo = new ServiceInfo(
			new ConnectionInfo("stagingServiceUrl", "serviceUser", "servicePassword"),
			new ConnectionInfo("databaseUrl", "databaseUser", "databasePassword"));
		
		provisioningManager.tell(new AddStagingService(stagingServiceInfo), ActorRef.noSender());
		f.ask(recorder, new Wait(1), Waited.class).get();
		
		ActorRef jobRecorder = actorSystem.actorOf(AnyRecorder.props(), "job-recorder");
		
		EnsureServiceJobInfo serviceJobInfo = new EnsureServiceJobInfo(0, "service");
		provisioningManager.tell(serviceJobInfo, jobRecorder);
		
		f.ask(jobRecorder, new Wait(2), Waited.class).get();
		f.ask(jobRecorder, new GetRecording(), Recording.class).get()
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.STARTED, update.getState());
			})
			.assertNext(Ack.class)
			.assertNotHasNext();
		
		f.ask(jobRecorder, new Clear(), Cleared.class).get();
		
		// provisioningManager should be busy (= not starting another job) 
		provisioningManager.tell(serviceJobInfo, jobRecorder);
		f.ask(jobRecorder, new Wait(1), Waited.class).get();
		f.ask(jobRecorder, new GetRecording(), Recording.class).get()
			.assertNext(Ack.class)
			.assertNotHasNext();
		
		f.ask(jobRecorder, new Clear(), Cleared.class).get();
		
		ActorSelection.apply(provisioningManager, "*").tell(new FinishJob(), ActorRef.noSender());
		
		f.ask(jobRecorder, new Wait(1), Waited.class).get();
		f.ask(jobRecorder, new GetRecording(), Recording.class).get()
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.SUCCEEDED, update.getState());
			})
			.assertNotHasNext();
	}

	@Test
	public void testServiceInfoUpdate() throws Exception {
		ServiceInfo stagingServiceInfo = new ServiceInfo(
			new ConnectionInfo("stagingServiceUrl", "serviceUser", "servicePassword"),
			new ConnectionInfo("databaseUrl", "databaseUser", "databasePassword"));
		
		ServiceInfo publicationServiceInfo = new ServiceInfo(
			new ConnectionInfo("publicationServiceUrl", "serviceUser", "servicePassword"),
			new ConnectionInfo("databaseUrl", "databaseUser", "databasePassword"));
		
		// we wait after every message to ensure a fixed order in the recording
		provisioningManager.tell(new AddStagingService(stagingServiceInfo), ActorRef.noSender());
		f.ask(recorder, new Wait(1), Waited.class).get();
		provisioningManager.tell(new AddPublicationService("environmentId", publicationServiceInfo), ActorRef.noSender());
		f.ask(recorder, new Wait(2), Waited.class).get();
		
		// services already registered -> should not have any effect
		provisioningManager.tell(new AddStagingService(stagingServiceInfo), ActorRef.noSender());
		provisioningManager.tell(new AddPublicationService("environmentId", publicationServiceInfo), ActorRef.noSender());
		
		provisioningManager.tell(new RemoveStagingService(stagingServiceInfo), ActorRef.noSender());
		f.ask(recorder, new Wait(3), Waited.class).get();
		provisioningManager.tell(new RemovePublicationService(publicationServiceInfo), ActorRef.noSender());
		f.ask(recorder, new Wait(4), Waited.class).get();
		
		f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(ServiceActorStarted.class, started -> {
				assertEquals(stagingServiceInfo, started.getServiceInfo());
				assertEquals("staging_data", started.getSchema());
			})
			.assertNext(ServiceActorStarted.class, started -> {
				assertEquals(publicationServiceInfo, started.getServiceInfo());
				assertEquals("data", started.getSchema());
			})
			.assertNext(ServiceActorStopped.class, stopped -> {
				assertEquals(stagingServiceInfo, stopped.getServiceInfo());
				assertEquals("staging_data", stopped.getSchema());
			})
			.assertNext(ServiceActorStopped.class, stopped -> {
				assertEquals(publicationServiceInfo, stopped.getServiceInfo());
				assertEquals("data", stopped.getSchema());
			})
			.assertNotHasNext();
	}
}
