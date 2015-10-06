package nl.idgis.publisher.service.provisioning;

import java.util.Collections;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.job.manager.messages.EnsureServiceJobInfo;
import nl.idgis.publisher.job.manager.messages.GetServiceJobs;
import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.job.manager.messages.VacuumServiceJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.manager.messages.PublishService;
import nl.idgis.publisher.service.provisioning.messages.AddPublicationService;
import nl.idgis.publisher.service.provisioning.messages.AddStagingService;
import nl.idgis.publisher.utils.TypedList;

import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class InitServiceJobCreatorTest extends AbstractServiceTest {
	
	private ActorRef initServiceJobCreator;
	
	@Before
	public void actor() {
		initServiceJobCreator = actorOf(InitServiceJobCreator.props(database, jobManager), "init-service-job-creator");
	}
	
	@Before
	public void databaseContent() throws Exception {
		int genericLayerId = insert(genericLayer)
			.set(genericLayer.name, "service-name")
			.set(genericLayer.identification, "service-id")
			.executeWithKey(genericLayer.id);
		
		insert(service)
			.set(service.genericLayerId, genericLayerId)
			.execute();
		
		insert(environment)
			.set(environment.identification, "environment-id")
			.set(environment.name, "environment-name")
			.execute();
		
		f.ask(serviceManager, new PublishService("service-id", Collections.singleton("environment-id")), Ack.class).get();
	}

	@Test
	public void testAddStagingService() throws Exception {
		TypedList<?> serviceJobs = f.ask(jobManager, new GetServiceJobs(), TypedList.class).get();
		assertFalse(serviceJobs.iterator().hasNext());
		
		f.ask(initServiceJobCreator, new AddStagingService(null), Ack.class).get();
						
		serviceJobs = f.ask(jobManager, new GetServiceJobs(), TypedList.class).get();
		
		Iterator<ServiceJobInfo> itr = serviceJobs.cast(ServiceJobInfo.class).iterator();
		assertTrue(itr.hasNext());
		
		ServiceJobInfo jobInfo = itr.next();
		assertTrue(jobInfo instanceof VacuumServiceJobInfo);
		assertFalse(jobInfo.isPublished());
		assertTrue(itr.hasNext());
		
		jobInfo = itr.next();
		assertTrue(jobInfo instanceof EnsureServiceJobInfo);
		assertFalse(jobInfo.isPublished());
		assertEquals("service-id", ((EnsureServiceJobInfo)jobInfo).getServiceId());
		assertFalse(itr.hasNext());
	}
	
	@Test
	public void testAddPublicationService() throws Exception {
		TypedList<?> serviceJobs = f.ask(jobManager, new GetServiceJobs(), TypedList.class).get();
		assertFalse(serviceJobs.iterator().hasNext());
		
		f.ask(initServiceJobCreator, new AddPublicationService("environment-id", null), Ack.class).get();
						
		serviceJobs = f.ask(jobManager, new GetServiceJobs(), TypedList.class).get();
		
		Iterator<ServiceJobInfo> itr = serviceJobs.cast(ServiceJobInfo.class).iterator();
		assertTrue(itr.hasNext());
		
		ServiceJobInfo jobInfo = itr.next();
		assertTrue(jobInfo instanceof VacuumServiceJobInfo);
		assertTrue(jobInfo.isPublished());
		assertTrue(itr.hasNext());
		
		jobInfo = itr.next();
		assertTrue(jobInfo instanceof EnsureServiceJobInfo);
		assertTrue(jobInfo.isPublished());
		assertEquals("service-id", ((EnsureServiceJobInfo)jobInfo).getServiceId());
		assertFalse(itr.hasNext());
	}
}
