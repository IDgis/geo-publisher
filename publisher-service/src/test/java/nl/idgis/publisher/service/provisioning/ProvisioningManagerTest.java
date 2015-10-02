package nl.idgis.publisher.service.provisioning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.DatabaseMock;
import nl.idgis.publisher.EmptyQueryResultTransactionMock;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.EnsureServiceJobInfo;
import nl.idgis.publisher.job.manager.messages.VacuumServiceJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.service.geoserver.messages.PreviousEnsureInfo;
import nl.idgis.publisher.service.manager.messages.GetPublishedService;
import nl.idgis.publisher.service.manager.messages.GetPublishedServiceIndex;
import nl.idgis.publisher.service.manager.messages.GetPublishedStyles;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.GetServiceIndex;
import nl.idgis.publisher.service.manager.messages.GetStyles;
import nl.idgis.publisher.service.manager.messages.PublishedServiceIndex;
import nl.idgis.publisher.service.manager.messages.ServiceIndex;
import nl.idgis.publisher.service.provisioning.messages.AddPublicationService;
import nl.idgis.publisher.service.provisioning.messages.AddStagingService;
import nl.idgis.publisher.service.provisioning.messages.GetEnvironments;
import nl.idgis.publisher.service.provisioning.messages.RemovePublicationService;
import nl.idgis.publisher.service.provisioning.messages.RemoveStagingService;
import nl.idgis.publisher.stream.IteratorCursor;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

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
			recorder.tell(msg, getSender());
			
			if(msg instanceof ServiceIndex) {
				getContext().parent().tell(new UpdateJobState(JobState.SUCCEEDED), getSelf());
			} else {
				unhandled(msg);
			}
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
			recorder.tell(msg, getSender());
			if(msg instanceof FinishJob) {
				targets.stream().forEach(target ->
					getContext().parent().tell(new UpdateJobState(JobState.SUCCEEDED), target));
				
				getContext().stop(getSelf());
			} else {
				unhandled(msg);
			}
		}
	};
	
	public static class ServiceManagerResponse {
		
		private final Object request;
		
		public ServiceManagerResponse(Object request) {
			this.request = request;
		}
		
		public Object getRequest() {
			return request;
		}
	}
	
	public static class ServiceManagerMock extends UntypedActor {
		
		private final ActorRef recorder;
		
		public ServiceManagerMock(ActorRef recorder) {
			this.recorder = recorder;
		}
		
		static Props props(ActorRef recorder) {
			return Props.create(ServiceManagerMock.class, recorder);
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			recorder.tell(msg, getSender());
			
			if(msg instanceof GetServiceIndex) {
				ServiceIndex serviceIndex = new ServiceIndex(
					Arrays.asList("service"), 
					Arrays.asList("style"));
				getSender().tell(serviceIndex, getSelf());
			} else if(msg instanceof GetPublishedServiceIndex) {
				PublishedServiceIndex publishedServiceIndex = new PublishedServiceIndex(
					"environmentId", 
					new ServiceIndex(
						Arrays.asList("service"), 
						Arrays.asList("style")));
				
				ActorRef cursor = getContext().actorOf(
					IteratorCursor.props(Collections.singleton(publishedServiceIndex).iterator()));
				
				cursor.tell(new NextItem(), getSender());
			} else {
				getSender().tell(new ServiceManagerResponse(msg), getSelf());
			}
		}
	}
	
	public static class EnvironmentInfoProviderMock extends UntypedActor {
		
		private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		private final ActorRef database;
		
		private FutureUtils f;
		
		private AsyncDatabaseHelper db;
		
		public EnvironmentInfoProviderMock(ActorRef database) {
			this.database = database;
		}
		
		public static Props props(ActorRef database) {
			return Props.create(EnvironmentInfoProviderMock.class, database);
		}
		
		@Override
		public void preStart() throws Exception {
			f = new FutureUtils(getContext());
			db = new AsyncDatabaseHelper(database, f, log);
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof GetEnvironments) {
				ActorRef sender = getSender();
				db.transactional((GetEnvironments)msg, tx ->
					f.successful(new TypedList<>(String.class, Arrays.asList("environmentId")))).thenAccept(resp ->
						sender.tell(resp, getSelf()));
			} else {
				unhandled(msg);
			}
		}
		
	}
	
	FutureUtils f;
	
	ActorSystem actorSystem;
	
	ActorRef serviceActorRecorder, jobActorRecorder, serviceManagerRecorder, provisioningManager;
	
	Path tempDir;
	
	@Before
	public void start() throws IOException {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		
		serviceActorRecorder = actorSystem.actorOf(AnyRecorder.props(), "serviceActorRecorder");
		jobActorRecorder = actorSystem.actorOf(AnyRecorder.props(), "jobActorRecorder");
		serviceManagerRecorder = actorSystem.actorOf(AnyRecorder.props(), "serviceManagerRecorder");
		
		provisioningManager = actorSystem.actorOf(
			ProvisioningManager.props(
				actorSystem.actorOf(DatabaseMock.props(EmptyQueryResultTransactionMock.props())),
				actorSystem.actorOf(ServiceManagerMock.props(serviceManagerRecorder)),
				
				new ProvisioningPropsFactory() {
					
					@Override
					public Props serviceProps(ServiceInfo serviceInfo, String schema) {
						return ServiceActor.props(serviceActorRecorder, serviceInfo, schema);
					}
					
					@Override
					public Props ensureJobProps(Set<ActorRef> targets) {
						return JobActor.props(jobActorRecorder, targets);
					}

					@Override
					public Props environmentInfoProviderProps(ActorRef database) {
						return EnvironmentInfoProviderMock.props(database);
					}
				}));
		
		f = new FutureUtils(actorSystem);
		
		tempDir = Files.createTempDirectory("provision-manager-test");
	}
	
	@After
	public void stop() {
		actorSystem.shutdown();
	}
	
	@Test
	public void testEnsureServiceJobInfoStaging() throws Exception {
		ServiceInfo stagingServiceInfo = new ServiceInfo(
			new ConnectionInfo("stagingServiceUrl", "serviceUser", "servicePassword"),
			tempDir.toString());
		
		f.ask(provisioningManager, new AddStagingService(stagingServiceInfo), Ack.class).get();
		f.ask(serviceActorRecorder, new Wait(1), Waited.class).get();
		
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
		
		f.ask(jobRecorder, new Clear(2), Cleared.class).get();
		
		// provisioningManager should be busy (= not starting another job) 
		provisioningManager.tell(serviceJobInfo, jobRecorder);
		f.ask(jobRecorder, new Wait(1), Waited.class).get();
		f.ask(jobRecorder, new GetRecording(), Recording.class).get()
			.assertNext(Ack.class)
			.assertNotHasNext();
		
		f.ask(jobRecorder, new Clear(1), Cleared.class).get();
		
		ActorSelection.apply(provisioningManager, "*").tell(new FinishJob(), ActorRef.noSender());
		
		f.ask(jobRecorder, new Wait(1), Waited.class).get();
		f.ask(jobRecorder, new GetRecording(), Recording.class).get()
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.SUCCEEDED, update.getState());
			})
			.assertNotHasNext();
		
		f.ask(serviceActorRecorder, new Wait(2), Waited.class).get();
		f.ask(serviceActorRecorder, new GetRecording(), Recording.class).get()
			.assertNext(ServiceActorStarted.class)
			.assertNext(FinishJob.class)
			.assertNotHasNext();
		
		Map<Class<?>, Object> serviceManagerRequests = new HashMap<>();
		f.ask(jobActorRecorder, new Wait(5), Waited.class).get();
		f.ask(jobActorRecorder, new GetRecording(), Recording.class).get()
			.assertNext(JobActorStarted.class)
			.assertNext(PreviousEnsureInfo.class)
			.assertNext(ServiceManagerResponse.class, resp -> {
				Object request = resp.getRequest();
				serviceManagerRequests.put(request.getClass(), request);
			})
			.assertNext(ServiceManagerResponse.class, resp -> {
				Object request = resp.getRequest();
				serviceManagerRequests.put(request.getClass(), request);
			})
			.assertNext(FinishJob.class)
			.assertNotHasNext();
		
		assertTrue(serviceManagerRequests.containsKey(GetService.class));
		GetService getService = (GetService)serviceManagerRequests.get(GetService.class);
		assertEquals("service", getService.getServiceId());
		
		assertTrue(serviceManagerRequests.containsKey(GetStyles.class));
		GetStyles getStyles = (GetStyles)serviceManagerRequests.get(GetStyles.class);
		assertEquals("service", getStyles.getServiceId());
	}
	
	@Test
	public void testEnsureServiceJobInfoPublished() throws Exception {
		ServiceInfo publicationServiceInfo = new ServiceInfo(
			new ConnectionInfo("publicationServiceUrl", "serviceUser", "servicePassword"),
			tempDir.toString());
		
		f.ask(provisioningManager, new AddPublicationService("environmentId", publicationServiceInfo), Ack.class).get();
		f.ask(serviceActorRecorder, new Wait(1), Waited.class).get();
		
		ActorRef jobRecorder = actorSystem.actorOf(AnyRecorder.props(), "job-recorder");
		
		EnsureServiceJobInfo serviceJobInfo = new EnsureServiceJobInfo(0, "service", true);
		provisioningManager.tell(serviceJobInfo, jobRecorder);
		
		f.ask(jobRecorder, new Wait(2), Waited.class).get();
		f.ask(jobRecorder, new GetRecording(), Recording.class).get()
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.STARTED, update.getState());
			})
			.assertNext(Ack.class)
			.assertNotHasNext();
		
		f.ask(jobRecorder, new Clear(2), Cleared.class).get();
		
		// provisioningManager should be busy (= not starting another job) 
		provisioningManager.tell(serviceJobInfo, jobRecorder);
		f.ask(jobRecorder, new Wait(1), Waited.class).get();
		f.ask(jobRecorder, new GetRecording(), Recording.class).get()
			.assertNext(Ack.class)
			.assertNotHasNext();
		
		f.ask(jobRecorder, new Clear(1), Cleared.class).get();
		
		ActorSelection.apply(provisioningManager, "*").tell(new FinishJob(), ActorRef.noSender());
		
		f.ask(jobRecorder, new Wait(1), Waited.class).get();
		f.ask(jobRecorder, new GetRecording(), Recording.class).get()
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.SUCCEEDED, update.getState());
			})
			.assertNotHasNext();
		
		f.ask(serviceActorRecorder, new Wait(2), Waited.class).get();
		f.ask(serviceActorRecorder, new GetRecording(), Recording.class).get()
			.assertNext(ServiceActorStarted.class)
			.assertNext(FinishJob.class)
			.assertNotHasNext();
		
		Map<Class<?>, Object> serviceManagerRequests = new HashMap<>();
		f.ask(jobActorRecorder, new Wait(5), Waited.class).get();
		f.ask(jobActorRecorder, new GetRecording(), Recording.class).get()
			.assertNext(JobActorStarted.class)
			.assertNext(PreviousEnsureInfo.class)
			.assertNext(ServiceManagerResponse.class, resp -> {
				Object request = resp.getRequest();
				serviceManagerRequests.put(request.getClass(), request);
			})
			.assertNext(ServiceManagerResponse.class, resp -> {
				Object request = resp.getRequest();
				serviceManagerRequests.put(request.getClass(), request);
			})
			.assertNext(FinishJob.class)
			.assertNotHasNext();
		
		assertTrue(serviceManagerRequests.containsKey(GetPublishedService.class));
		GetPublishedService getService = (GetPublishedService)serviceManagerRequests.get(GetPublishedService.class);
		assertEquals("service", getService.getServiceId());
		
		assertTrue(serviceManagerRequests.containsKey(GetPublishedStyles.class));
		GetPublishedStyles getStyles = (GetPublishedStyles)serviceManagerRequests.get(GetPublishedStyles.class);
		assertEquals("service", getStyles.getServiceId());
	}

	@Test
	public void testServiceInfoUpdate() throws Exception {
		ServiceInfo stagingServiceInfo = new ServiceInfo(
			new ConnectionInfo("stagingServiceUrl", "serviceUser", "servicePassword"),
			tempDir.toString());
		
		ServiceInfo publicationServiceInfo = new ServiceInfo(
			new ConnectionInfo("publicationServiceUrl", "serviceUser", "servicePassword"),
			tempDir.toString());
		
		// we wait after every message to ensure a fixed order in the recording
		f.ask(provisioningManager, new AddStagingService(stagingServiceInfo), Ack.class).get();
		f.ask(serviceActorRecorder, new Wait(1), Waited.class).get();
		f.ask(provisioningManager, new AddPublicationService("environmentId", publicationServiceInfo), Ack.class).get();
		f.ask(serviceActorRecorder, new Wait(2), Waited.class).get();
		
		// services already registered -> should not have any effect
		f.ask(provisioningManager, new AddStagingService(stagingServiceInfo), Ack.class).get();
		f.ask(provisioningManager, new AddPublicationService("environmentId", publicationServiceInfo), Ack.class).get();
		
		f.ask(provisioningManager, new RemoveStagingService(stagingServiceInfo), Ack.class).get();
		f.ask(serviceActorRecorder, new Wait(3), Waited.class).get();
		f.ask(provisioningManager, new RemovePublicationService(publicationServiceInfo), Ack.class).get();
		f.ask(serviceActorRecorder, new Wait(4), Waited.class).get();
		
		f.ask(serviceActorRecorder, new GetRecording(), Recording.class).get()
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
	
	@Test
	public void testVacuumServiceJobInfoStaging() throws Exception {
		ServiceInfo stagingServiceInfo = new ServiceInfo(
			new ConnectionInfo("stagingServiceUrl", "serviceUser", "servicePassword"),
			tempDir.toString());

		f.ask(provisioningManager, new AddStagingService(stagingServiceInfo), Ack.class).get();		
		f.ask(serviceActorRecorder, new Wait(1), Waited.class).get();
			
		ActorRef jobRecorder = actorSystem.actorOf(AnyRecorder.props(), "job-recorder");
		
		VacuumServiceJobInfo serviceJobInfo = new VacuumServiceJobInfo(0);
		provisioningManager.tell(serviceJobInfo, jobRecorder);
		
		f.ask(serviceActorRecorder, new Wait(2), Waited.class).get();
		f.ask(serviceActorRecorder, new GetRecording(), Recording.class).get()
			.assertNext(ServiceActorStarted.class)
			.assertNext(ServiceIndex.class)
			.assertNotHasNext();
		
		f.ask(jobRecorder, new Wait(3), Waited.class).get();
		f.ask(jobRecorder, new GetRecording(), Recording.class).get()
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.STARTED, update.getState());
			})
			.assertNext(Ack.class)
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.SUCCEEDED, update.getState());
			})
			.assertNotHasNext();
	}
	
	@Test
	public void testVacuumServiceJobInfoPublished() throws Exception {
		ServiceInfo publicationServiceInfo = new ServiceInfo(
				new ConnectionInfo("publicationServiceUrl", "serviceUser", "servicePassword"),
				tempDir.toString());
			
		f.ask(provisioningManager, new AddPublicationService("environmentId", publicationServiceInfo), Ack.class).get();
		f.ask(serviceActorRecorder, new Wait(1), Waited.class).get();
			
		ActorRef jobRecorder = actorSystem.actorOf(AnyRecorder.props(), "job-recorder");
		
		VacuumServiceJobInfo serviceJobInfo = new VacuumServiceJobInfo(0, true);
		provisioningManager.tell(serviceJobInfo, jobRecorder);
		
		f.ask(serviceActorRecorder, new Wait(2), Waited.class).get();
		f.ask(serviceActorRecorder, new GetRecording(), Recording.class).get()
			.assertNext(ServiceActorStarted.class)
			.assertNext(ServiceIndex.class)
			.assertNotHasNext();
		
		f.ask(jobRecorder, new Wait(3), Waited.class).get();
		f.ask(jobRecorder, new GetRecording(), Recording.class).get()
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.STARTED, update.getState());
			})
			.assertNext(Ack.class)
			.assertNext(UpdateJobState.class, update -> {
				assertEquals(JobState.SUCCEEDED, update.getState());
			})
			.assertNotHasNext();
	}
}
