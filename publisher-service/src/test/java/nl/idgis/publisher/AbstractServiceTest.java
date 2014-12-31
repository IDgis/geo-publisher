package nl.idgis.publisher;

import static org.junit.Assert.assertTrue;

import org.junit.Before;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.AbstractDatabaseTest;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.UpdateJobState;

import nl.idgis.publisher.dataset.DatasetManager;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.job.JobManager;
import nl.idgis.publisher.job.messages.JobManagerRequest;
import nl.idgis.publisher.utils.TypedIterable;

public abstract class AbstractServiceTest extends AbstractDatabaseTest {
	
	protected ActorRef jobManager;
	
	protected ActorRef datasetManager;
	
	@Before
	public void jobManager() {
		jobManager = actorOf(JobManager.props(database), "jobManager");
	}
	
	@Before
	public void datasetManager() {
		datasetManager = actorOf(DatasetManager.props(database), "datasetManager");
	}
	
	protected void executeJobs(JobManagerRequest request) throws Exception {
		TypedIterable<?> iterable = sync.ask(jobManager, request, TypedIterable.class);
		assertTrue(iterable.contains(JobInfo.class));
		for(JobInfo job : iterable.cast(JobInfo.class)) {
			sync.ask(database, new UpdateJobState(job, JobState.SUCCEEDED));
		}
	}
}
