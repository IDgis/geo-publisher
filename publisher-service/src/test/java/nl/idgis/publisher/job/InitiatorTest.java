package nl.idgis.publisher.job;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AbstractDatabaseTest;
import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.CreateServiceJob;
import nl.idgis.publisher.database.messages.HarvestJobInfo;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.ServiceJobInfo;

import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;

public class InitiatorTest extends AbstractDatabaseTest {
	
	static class GetLastReceivedJob {
		
	}
	
	static class JobReceiver extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		ActorRef sender = null;
		JobInfo lastJob = null;

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("message received: " + msg);
			
			if(msg instanceof JobInfo) {
				lastJob = (JobInfo)msg;				
				sendLastJob();
			} else if(msg instanceof GetLastReceivedJob) {
				sender = getSender();
				sendLastJob();	 
			}
		}
		
		private void sendLastJob() {
			if(lastJob != null && sender != null) {
				sender.tell(lastJob, getSelf());
			}
		}
		
	}
	
	ActorRef initiator;
	
	ActorRef harvester;
	ActorRef loader;
	ActorRef service;
	
	@Before
	public void databaseContent() throws Exception {
		insertDataSource();
		
		Dataset testDataset = createTestDataset();
		ask(database, new RegisterSourceDataset("testDataSource", testDataset));
		
		Table testTable = testDataset.getTable();
		ask(database, new CreateDataset("testDataset", "My Test Dataset", testDataset.getId(), testTable.getColumns(), ""));
	}
	
	@Before	
	public void actors() throws Exception {	
		Props jobReceiverProps = Props.create(JobReceiver.class);
		
		harvester = actorOf(jobReceiverProps, "harvesterMock");
		loader = actorOf(jobReceiverProps, "loaderMock");
		service = actorOf(jobReceiverProps, "serviceMock");
		
		initiator = actorOf(Initiator.props(database, harvester, loader, service), "initiator");
	}
	
	@Test
	public void testHarvestJob() throws Exception {
		ask(database, new CreateHarvestJob("testDataSource"));		
		askAssert(harvester, new GetLastReceivedJob(), HarvestJobInfo.class);
	}
	
	@Test
	public void testImportJob() throws Exception {
		ask(database, new CreateImportJob("testDataset"));		
		askAssert(loader, new GetLastReceivedJob(), ImportJobInfo.class);
	}
	
	@Test
	public void testServiceJob() throws Exception {
		ask(database, new CreateServiceJob("testDataset"));		
		askAssert(service, new GetLastReceivedJob(), ServiceJobInfo.class);
	}
}
