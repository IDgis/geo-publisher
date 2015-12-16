package nl.idgis.publisher.service.provisioning;

import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.AllForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActorWithStash;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.job.context.JobContext;
import nl.idgis.publisher.job.context.messages.JobFinished;
import nl.idgis.publisher.job.manager.messages.CreateEnsureServiceJob;
import nl.idgis.publisher.job.manager.messages.GetServiceJobs;
import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.service.geoserver.rest.DataStore;
import nl.idgis.publisher.service.geoserver.rest.GeoServerException;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRestFactory;
import nl.idgis.publisher.service.geoserver.rest.Workspace;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.provisioning.messages.AddStagingService;
import nl.idgis.publisher.utils.TypedList;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

public class ProvisioningManagerCrashTest extends AbstractServiceTest {
	
	private UniqueNameGenerator nameGenerator;
	
	public static class TestProvisioningSystem extends UntypedActorWithStash {
		
		private final static SupervisorStrategy supervisorStrategy = new AllForOneStrategy(10, Duration.create("1 minute"), 
			new Function<Throwable, Directive>() {

			@Override
			public Directive apply(Throwable t) throws Exception {
				return AllForOneStrategy.restart();
			}
			
		});
		
		private final GeoServerRest rest;
		
		private final ActorRef database, serviceManager;
		
		private ActorRef provisioningManager;
		
		public TestProvisioningSystem(GeoServerRest rest, ActorRef database, ActorRef serviceManager) {
			this.rest = rest;
			this.database = database;
			this.serviceManager = serviceManager;
		}
		
		public static Props props(GeoServerRest rest, ActorRef database, ActorRef serviceManager) {
			return Props.create(TestProvisioningSystem.class, rest, database, serviceManager);
		}
		
		@Override
		public void preStart() throws Exception {
			// wire mock in provisioning manager
			ProvisioningPropsFactory propsFactory = new GeoServerProvisioningPropsFactory() {
				
				@Override
				protected GeoServerRestFactory restFactory(ConnectionInfo serviceConnectionInfo) {
					return (f, log) -> rest;
				}
			};
			
			// start provisioning manager
			Config config = ConfigFactory.empty();
			provisioningManager = getContext().actorOf(
				ProvisioningManager.props(database, serviceManager, propsFactory, config),
				"provisioning-manager");
			
			// register mock staging service
			provisioningManager.tell( 
				new AddStagingService(
					new ServiceInfo(
						new ConnectionInfo("http://example.com/geoserver", "user", "password"), "/raster")), 
				getSelf());
		}
		
		public Procedure<Object> started() {
			return new Procedure<Object>() {

				@Override
				public void apply(Object msg) throws Exception {
					provisioningManager.forward(msg, getContext());
				}
				
			};
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof Ack) {
				unstashAll();
				getContext().become(started());
			} else {
				stash();
			}
			
		}
		
		@Override
		public SupervisorStrategy supervisorStrategy() {
			return supervisorStrategy;
		}
	}
	
	@Before
	public void setUp() throws Exception {
		nameGenerator = new UniqueNameGenerator();
	}
	
	@Before
	public void prepareDb() throws Exception {
		// create service
		int genericLayerId = insert(genericLayer)
			.set(genericLayer.identification, "serviceId")
			.set(genericLayer.name, "serviceName")
			.executeWithKey(genericLayer.id);
		
		insert(service)
			.set(service.genericLayerId, genericLayerId)
			.execute();
		
		// assert created service
		f.ask(serviceManager, new GetService("serviceId"), Service.class).get();
	}
	
	@Test
	public void testGeoServerFailure() throws Exception {
		// mock for broken staging service (i.e. facade is throwing exceptions)
		GeoServerRest restMock = mock(GeoServerRest.class);
		Workspace workspace = new Workspace("serviceName");
		DataStore dataStore = new DataStore("publisher-geometry", Collections.emptyMap());
		
		when(restMock.getWorkspace("serviceName"))
			.thenReturn(f.successful(Optional.of(workspace)));
		when(restMock.getDataStore(workspace, "publisher-geometry"))
			.thenReturn(f.successful(Optional.of(dataStore)));
		when(restMock.getFeatureTypes(workspace, dataStore))
			.thenReturn(f.failed(new GeoServerException(new RuntimeException("simulated issue"))));
		
		ActorRef provisioningSystem = actorOf(
			TestProvisioningSystem.props(restMock, database, serviceManager), 
			"provisioning-system");
		
		performEnsureJob(provisioningSystem);
		performEnsureJob(provisioningSystem);
	}
	
	@Test
	public void testCrash() throws Exception {
		// empty mock (causes NPE)
		GeoServerRest restMock = mock(GeoServerRest.class);
		
		ActorRef provisioningSystem = actorOf(
			TestProvisioningSystem.props(restMock, database, serviceManager), 
			"provisioning-system");
		
		performEnsureJob(provisioningSystem);
		performEnsureJob(provisioningSystem);
	}
	
	static class ProvisioningSystem {
		
		final ActorRef helperRecorder, testProvisioningSystem;
		
		ProvisioningSystem(ActorRef helperRecorder, ActorRef testProvisioningSystem) {
			this.helperRecorder = helperRecorder;
			this.testProvisioningSystem = testProvisioningSystem;
		}
	}

	private void performEnsureJob(ActorRef provisioningSystem) throws Exception {
		// create ensure service job
		f.ask(jobManager, new CreateEnsureServiceJob("serviceId"), Ack.class).get();
		
		// retrieve and execute ensure job
		Iterator<ServiceJobInfo> itr = ((TypedList<?>)f.ask(
				jobManager, 
				new GetServiceJobs(), 
				TypedList.class).get())
			.cast(ServiceJobInfo.class)
			.iterator();
		
		assertTrue(itr.hasNext());
		ServiceJobInfo serviceJobInfo = itr.next();
		assertFalse(itr.hasNext());
		assertNotNull(serviceJobInfo);
		
		ActorRef jobListenerRecorder = actorOf(AnyRecorder.props(), nameGenerator.getName("recorder"));
		ActorRef jobContext = actorOf(JobContext.props(provisioningSystem, jobListenerRecorder, serviceJobInfo), "job-context");
		provisioningSystem.tell(serviceJobInfo, jobContext);
		
		// job should fail
		f.ask(jobListenerRecorder, new Wait(1), Waited.class).get();
		f.ask(jobListenerRecorder, new GetRecording(), Recording.class).get()
			.assertNext(JobFinished.class, jobFinished -> 
				assertEquals(JobState.FAILED, jobFinished.getJobState()));
		
		jobListenerRecorder.tell(PoisonPill.getInstance(), ActorRef.noSender());
	}
}
