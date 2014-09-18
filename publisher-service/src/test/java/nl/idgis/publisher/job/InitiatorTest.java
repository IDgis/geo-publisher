package nl.idgis.publisher.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.database.AbstractDatabaseTest;
import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.CreateServiceJob;
import nl.idgis.publisher.database.messages.HarvestJobInfo;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.ServiceJobInfo;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.protocol.messages.Ack;

import static nl.idgis.publisher.utils.TestPatterns.ask;
import static nl.idgis.publisher.utils.TestPatterns.askAssert;

public class InitiatorTest extends AbstractDatabaseTest {
	
	static class GetReceivedJobs {
		
		final int count;
		
		GetReceivedJobs(int count) {
			this.count = count;
		}
		
		int getCount() {
			return count;
		}

		@Override
		public String toString() {
			return "GetReceivedJobs [count=" + count + "]";
		}
		
	}
	
	static class JobReceiver extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		Integer count = null;
		ActorRef sender = null;
		List<JobInfo> jobs = new ArrayList<>();
		
		final ActorRef database;
		
		JobReceiver(ActorRef database) {
			this.database = database;
		}
		
		Procedure<Object> waitingForDatabaseAck(final JobInfo job, final ActorRef initiator) {
			return new Procedure<Object>() {

				@Override
				public void apply(Object msg) throws Exception {
					if(msg instanceof Ack) {
						jobs.add(job);				
						sendJobs();
						
						initiator.tell(new Ack(), getSelf());
						getContext().unbecome();
					} else {
						onElse(msg);
					}
				}
				
			};
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("message received: " + msg);
			
			if(msg instanceof JobInfo) {
				database.tell(new UpdateJobState((JobInfo)msg, JobState.SUCCEEDED), getSelf());
				
				getContext().become(waitingForDatabaseAck((JobInfo)msg, getSender()));
			} else {
				onElse(msg);
			}
		}
		
		void onElse(Object msg) {
			if(msg instanceof GetReceivedJobs) {
				sender = getSender();
				count = ((GetReceivedJobs) msg).getCount();
				sendJobs();	 
			} else {
				unhandled(msg);
			}
		}
		
		void sendJobs() {
			if(sender != null && count != null && jobs.size() == count) {
				sender.tell(jobs, getSelf());
				
				sender = null;
				count = null;
				jobs = new ArrayList<>();
			}
		}
		
	}
	
	ActorRef harvester;
	ActorRef loader;
	ActorRef service;
	
	@Before
	public void databaseContent() throws Exception {
		insertDataset("testDataset0");
		insertDataset("testDataset1");
	}	
	
	@Before	
	public void actors() throws Exception {	
		Props jobReceiverProps = Props.create(JobReceiver.class, database);
		
		harvester = actorOf(jobReceiverProps, "harvesterMock");
		loader = actorOf(jobReceiverProps, "loaderMock");
		service = actorOf(jobReceiverProps, "serviceMock");
	}

	private void initInitiator() {
		actorOf(Initiator.props(database, harvester, loader, service), "initiator");
	}
	
	@Test
	public void testHarvestJob() throws Exception {
		ask(database, new CreateHarvestJob("testDataSource"));
		initInitiator();
		
		List<?> list = askAssert(harvester, new GetReceivedJobs(1), List.class);
		assertEquals(HarvestJobInfo.class, list.get(0).getClass());
	}
	
	@Test
	public void testImportJob() throws Exception {
		ask(database, new CreateImportJob("testDataset0"));
		ask(database, new CreateImportJob("testDataset1"));
		initInitiator();
		
		List<?> list = askAssert(loader, new GetReceivedJobs(2), List.class);
		
		Object job0 = list.get(0);
		Object job1 = list.get(1);
		
		assertEquals(ImportJobInfo.class, job0.getClass());
		assertEquals(ImportJobInfo.class, job1.getClass());
		
		Set<String> datasets = new HashSet<>();
		datasets.add(((ImportJobInfo)job0).getDatasetId());
		datasets.add(((ImportJobInfo)job1).getDatasetId());
		
		assertTrue(datasets.contains("testDataset0"));
		assertTrue(datasets.contains("testDataset1"));
	}
	
	@Test
	public void testServiceJob() throws Exception {
		ask(database, new CreateServiceJob("testDataset0"));
		initInitiator();
		
		List<?> list = askAssert(service, new GetReceivedJobs(1), List.class);
		assertEquals(ServiceJobInfo.class, list.get(0).getClass());
	}
	
	@Test
	public void testTimeout() throws Exception {
		actorOf(Initiator.props(database, harvester, loader, service, Duration.create(1, TimeUnit.MILLISECONDS)), "initiator");
		
		Thread.sleep(100);
		
		ask(database, new CreateServiceJob("testDataset0"));
		askAssert(service, new GetReceivedJobs(1), List.class);
		
		Thread.sleep(100);
		
		ask(database, new CreateServiceJob("testDataset0"));
		askAssert(service, new GetReceivedJobs(1), List.class);
		
		Thread.sleep(100);
		
		ask(database, new CreateServiceJob("testDataset0"));
		askAssert(service, new GetReceivedJobs(1), List.class);
	}
}
