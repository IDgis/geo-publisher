package nl.idgis.publisher.job;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.job.messages.CreateHarvestJob;
import nl.idgis.publisher.job.messages.CreateImportJob;
import nl.idgis.publisher.job.messages.CreateServiceJob;
import nl.idgis.publisher.job.messages.GetHarvestJobs;
import nl.idgis.publisher.job.messages.GetImportJobs;
import nl.idgis.publisher.job.messages.JobManagerRequest;

import static nl.idgis.publisher.utils.TestPatterns.ask;
import static nl.idgis.publisher.utils.TestPatterns.askAssert;

public class CreatorTest extends AbstractServiceTest {
	
	static class GetLastReceivedRequest {
		
	}
	
	static class ManagerAdapter extends UntypedActor {
		
		final ActorRef manager;
		
		JobManagerRequest lastRequest = null;
		ActorRef sender = null;
		
		public ManagerAdapter(ActorRef manager) {
			this.manager = manager;
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof GetLastReceivedRequest) {
				sender = getSender();
				sendLastRequest();				
			} else {			
				if(msg instanceof CreateHarvestJob
					|| msg instanceof CreateImportJob
					|| msg instanceof CreateServiceJob) {
					
					lastRequest = (JobManagerRequest)msg;
					sendLastRequest();
				}
					
				manager.tell(msg, getSender());
			}
		}
		
		private void sendLastRequest() {
			if(sender != null && lastRequest != null) {
				sender.tell(lastRequest, getSelf());
			}
		}
		
	}
	
	ActorRef managerAdapter;
	
	@Before
	public void actors() {		
		managerAdapter = actorOf(Props.create(ManagerAdapter.class, jobManager), "managerAdapter");		
	}

	private void initCreator() {
		actorOf(Creator.props(managerAdapter, database), "creator");
	}
	
	
	
	private void harvest() throws Exception {
		ask(jobManager, new CreateHarvestJob("testDataSource"));		
		executeJobs(new GetHarvestJobs());
	}

	@Test
	public void testHarvestJob() throws Exception {
		insertDataSource();
		initCreator();
		askAssert(managerAdapter, new GetLastReceivedRequest(), CreateHarvestJob.class);
	}
	
	@Test
	public void testImportJob() throws Exception {
		insertDataset();
		harvest();
		initCreator();
		askAssert(managerAdapter, new GetLastReceivedRequest(), CreateImportJob.class);
	}	
	
	@Test
	public void testServiceJob() throws Exception {
		insertDataset();
		harvest();
		ask(jobManager, new CreateImportJob("testDataset"));
		executeJobs(new GetImportJobs());
		initCreator();
		askAssert(managerAdapter, new GetLastReceivedRequest(), CreateServiceJob.class);
	}
}
