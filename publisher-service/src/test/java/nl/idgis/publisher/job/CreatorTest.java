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
import nl.idgis.publisher.database.messages.Query;

public class CreatorTest extends AbstractDatabaseTest {
	
	static class GetLastReceivedQuery {
		
	}
	
	static class DatabaseAdapter extends UntypedActor {
		
		final ActorRef database;
		
		Query lastQuery = null;
		ActorRef sender = null;
		
		public DatabaseAdapter(ActorRef database) {
			this.database = database;
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
					
				database.tell(msg, getSender());
			}
		}
		
		private void sendLastQuery() {
			if(sender != null && lastQuery != null) {
				sender.tell(lastQuery, getSelf());
			}
		}
		
	}
	
	ActorRef databaseAdapter;
	
	@Before
	public void actors() {
		databaseAdapter = actorOf(Props.create(DatabaseAdapter.class, database), "databaseAdapter");		
	}

	private void initCreator() {
		actorOf(Creator.props(databaseAdapter), "creator");
	}
	
	private void harvest() throws Exception {
		ask(database, new CreateHarvestJob("testDataSource"));		
		executeJobs(new GetHarvestJobs());
	}

	@Test
	public void testHarvestJob() throws Exception {
		insertDataSource();
		initCreator();
		askAssert(databaseAdapter, new GetLastReceivedQuery(), CreateHarvestJob.class);
	}
	
	@Test
	public void testImportJob() throws Exception {
		insertDataset();
		harvest();
		initCreator();
		askAssert(databaseAdapter, new GetLastReceivedQuery(), CreateImportJob.class);
	}	
	
	@Test
	public void testServiceJob() throws Exception {
		insertDataset();
		harvest();
		ask(database, new CreateImportJob("testDataset"));
		executeJobs(new GetImportJobs());
		initCreator();
		askAssert(databaseAdapter, new GetLastReceivedQuery(), CreateServiceJob.class);
	}
}
