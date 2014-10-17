package nl.idgis.publisher.job;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import nl.idgis.publisher.database.AbstractDatabaseTest;
import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.CreateServiceJob;
import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.utils.TypedIterable;
import static nl.idgis.publisher.utils.TestPatterns.ask;
import static nl.idgis.publisher.utils.TestPatterns.askAssert;
import static org.junit.Assert.assertTrue;

public class CreatorTest extends AbstractDatabaseTest {
	
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
	
	ActorRef manager, managerAdapter;
	
	@Before
	public void actors() {
		manager = actorOf(JobManager.props(database), "manager");
		managerAdapter = actorOf(Props.create(ManagerAdapter.class, manager), "managerAdapter");		
	}

	private void initCreator() {
		actorOf(Creator.props(managerAdapter), "creator");
	}
	
	@Override
	protected void executeJobs(Query query) throws Exception {
		TypedIterable<?> iterable = askAssert(manager, query, TypedIterable.class);
		assertTrue(iterable.contains(JobInfo.class));
		for(JobInfo job : iterable.cast(JobInfo.class)) {
			ask(database, new UpdateJobState(job, JobState.SUCCEEDED));
		}
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
