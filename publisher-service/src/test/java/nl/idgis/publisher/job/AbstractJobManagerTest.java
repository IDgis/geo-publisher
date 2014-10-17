package nl.idgis.publisher.job;

import static nl.idgis.publisher.utils.TestPatterns.ask;
import static nl.idgis.publisher.utils.TestPatterns.askAssert;
import static org.junit.Assert.assertTrue;

import org.junit.Before;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.AbstractDatabaseTest;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.utils.TypedIterable;

public abstract class AbstractJobManagerTest extends AbstractDatabaseTest {
	
	protected ActorRef manager;
	
	@Before
	public void manager() {
		manager = actorOf(JobManager.props(database), "manager");				
	}

	@Override
	protected void executeJobs(Query query) throws Exception {
		TypedIterable<?> iterable = askAssert(manager, query, TypedIterable.class);
		assertTrue(iterable.contains(JobInfo.class));
		for(JobInfo job : iterable.cast(JobInfo.class)) {
			ask(database, new UpdateJobState(job, JobState.SUCCEEDED));
		}
	}
}
