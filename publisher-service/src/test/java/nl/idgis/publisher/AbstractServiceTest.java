package nl.idgis.publisher;

import static org.junit.Assert.assertTrue;

import org.junit.Before;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.AbstractDatabaseTest;

import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;

import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.UpdateState;

import nl.idgis.publisher.dataset.DatasetManager;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.VectorDataset;

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
			sync.ask(database, new UpdateState(job, JobState.SUCCEEDED));
		}
	}
	
	protected void insertDataset() throws Exception {
		insertDataset("testDataset");
	}
	
	protected void insertDataset(String datasetId) throws Exception {
		insertDataSource();
		
		VectorDataset testDataset = createVectorDataset();
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", testDataset));
		
		Table testTable = testDataset.getTable();
		sync.ask(database, new CreateDataset(
				datasetId, 
				"My Test Dataset", 
				testDataset.getId(), 
				testTable.getColumns(), 
				"{ \"expression\": null }"));
	}
}
