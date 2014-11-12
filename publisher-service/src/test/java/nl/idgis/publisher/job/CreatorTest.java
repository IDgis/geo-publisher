package nl.idgis.publisher.job;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.CreateServiceJob;
import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.Query;

import static nl.idgis.publisher.utils.TestPatterns.ask;
import static nl.idgis.publisher.utils.TestPatterns.askAssert;

public class CreatorTest extends AbstractJobManagerTest {
	
	static class GetLastReceivedQuery {
		
	}
	
	static class ManagerAdapter extends UntypedActor {
		
		final ActorRef manager;
		
		Query lastQuery = null;
		ActorRef sender = null;
		
		public ManagerAdapter(ActorRef manager) {
			this.manager = manager;
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof GetLastReceivedQuery) {
				sender = getSender();
				sendLastQuery();				
			} else {			
				if(msg instanceof CreateHarvestJob
					|| msg instanceof CreateImportJob
					|| msg instanceof CreateServiceJob) {
					
					lastQuery = (Query)msg;
					sendLastQuery();
				}
					
				manager.tell(msg, getSender());
			}
		}
		
		private void sendLastQuery() {
			if(sender != null && lastQuery != null) {
				sender.tell(lastQuery, getSelf());
			}
		}
		
	}
	
	ActorRef managerAdapter;
	
	@Before
	public void actors() {		
		managerAdapter = actorOf(Props.create(ManagerAdapter.class, manager), "managerAdapter");		
	}

	private void initCreator() {
		actorOf(Creator.props(managerAdapter, database), "creator");
	}
	
	
	
	private void harvest() throws Exception {
		ask(manager, new CreateHarvestJob("testDataSource"));		
		executeJobs(new GetHarvestJobs());
	}

	@Test
	public void testHarvestJob() throws Exception {
		insertDataSource();
		initCreator();
		askAssert(managerAdapter, new GetLastReceivedQuery(), CreateHarvestJob.class);
	}
	
	@Test
	public void testImportJob() throws Exception {
		insertDataset();
		harvest();
		initCreator();
		askAssert(managerAdapter, new GetLastReceivedQuery(), CreateImportJob.class);
	}	
	
	@Test
	public void testServiceJob() throws Exception {
		insertDataset();
		harvest();
		ask(manager, new CreateImportJob("testDataset"));
		executeJobs(new GetImportJobs());
		initCreator();
		askAssert(managerAdapter, new GetLastReceivedQuery(), CreateServiceJob.class);
	}
}
