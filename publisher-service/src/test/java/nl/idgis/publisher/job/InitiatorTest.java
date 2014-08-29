package nl.idgis.publisher.job;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AbstractDatabaseTest;
import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.CreateServiceJob;
import nl.idgis.publisher.database.messages.HarvestJobInfo;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.ServiceJobInfo;

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
	
	ActorRef harvester;
	ActorRef loader;
	ActorRef service;
	
	@Before
	public void databaseContent() throws Exception {
		insertDataset();
	}	
	
	@Before	
	public void actors() throws Exception {	
		Props jobReceiverProps = Props.create(JobReceiver.class);
		
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
		askAssert(harvester, new GetLastReceivedJob(), HarvestJobInfo.class);
	}
	
	@Test
	public void testImportJob() throws Exception {
		ask(database, new CreateImportJob("testDataset"));
		initInitiator();
		askAssert(loader, new GetLastReceivedJob(), ImportJobInfo.class);
	}
	
	@Test
	public void testServiceJob() throws Exception {
		ask(database, new CreateServiceJob("testDataset"));
		initInitiator();
		askAssert(service, new GetLastReceivedJob(), ServiceJobInfo.class);
	}
}
