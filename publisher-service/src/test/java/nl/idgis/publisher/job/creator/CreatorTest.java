package nl.idgis.publisher.job.creator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.job.creator.messages.CreateHarvestJobs;
import nl.idgis.publisher.job.creator.messages.CreateImportJobs;
import nl.idgis.publisher.job.manager.messages.GetHarvestJobs;
import nl.idgis.publisher.job.manager.messages.GetImportJobs;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.TypedList;

public class CreatorTest extends AbstractServiceTest {
	
	ActorRef creator;
	
	@Before
	public void actors() {		
		creator = actorOf(Creator.props(jobManager, database), "creator");
	}

	@Test
	public void testHarvestJob() throws Exception {
		insertDataSource();
		
		assertFalse(f.ask(jobManager, new GetHarvestJobs(), TypedList.class).get().iterator().hasNext());
		f.ask(creator, new CreateHarvestJobs(), Ack.class).get();
		assertTrue(f.ask(jobManager, new GetHarvestJobs(), TypedList.class).get().iterator().hasNext());
	}
	
	@Test
	public void testImportJob() throws Exception {
		insertDataset();
				
		assertFalse(f.ask(jobManager, new GetImportJobs(), TypedList.class).get().iterator().hasNext());
		f.ask(creator, new CreateImportJobs(), Ack.class).get();		
		assertTrue(f.ask(jobManager, new GetImportJobs(), TypedList.class).get().iterator().hasNext());
	}
}
