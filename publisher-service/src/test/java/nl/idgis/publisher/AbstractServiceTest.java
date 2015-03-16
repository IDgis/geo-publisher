package nl.idgis.publisher;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Before;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.AbstractDatabaseTest;

import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;

import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.dataset.DatasetManager;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.VectorDataset;

import nl.idgis.publisher.job.manager.JobManager;
import nl.idgis.publisher.job.manager.messages.JobManagerRequest;
import nl.idgis.publisher.job.manager.messages.UpdateState;
import nl.idgis.publisher.service.manager.ServiceManager;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedIterable;

public abstract class AbstractServiceTest extends AbstractDatabaseTest {
	
	protected ActorRef jobManager;
	
	protected ActorRef datasetManager;
	
	protected ActorRef serviceManager;
	
	@Before
	public void jobManager() {
		jobManager = actorOf(JobManager.props(database), "jobManager");
	}
	
	@Before
	public void datasetManager() {
		datasetManager = actorOf(DatasetManager.props(database), "datasetManager");
	}
	
	@Before
	public void serviceManager() {
		serviceManager = actorOf(ServiceManager.props(database), "serviceManager");
	}
	
	protected void executeJobs(JobManagerRequest request) throws Exception {
		TypedIterable<?> iterable = f.ask(jobManager, request, TypedIterable.class).get();
		assertTrue(iterable.contains(JobInfo.class));
		for(JobInfo job : iterable.cast(JobInfo.class)) {
			f.ask(jobManager, new UpdateState(job, JobState.SUCCEEDED)).get();
		}
	}
	
	protected void insertDataset() throws Exception {
		insertDataset("testDataset");
	}
	
	protected void insertDataset(String datasetId) throws Exception {
		insertDataSource();
		
		VectorDataset testDataset = createVectorDataset();
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", testDataset)).get();
		
		Table testTable = testDataset.getTable();
		createDataset(
				datasetId, 
				"My Test Dataset", 
				testDataset.getId(), 
				testTable.getColumns(), 
				"{ \"expression\": null }");
	}
	
	protected void createDataset(String datasetIdentification, String datasetName, 
		String sourceDatasetIdentification, List<Column> columnList, String filterConditions) {
		
		int sourceDatasetId = 
			query().from(sourceDataset)
			.where(sourceDataset.identification.eq(sourceDatasetIdentification))
			.singleResult(sourceDataset.id);
		
		int datasetId = insert(dataset)
			.set(dataset.identification, datasetIdentification)
			.set(dataset.uuid, UUID.randomUUID().toString())
			.set(dataset.fileUuid, UUID.randomUUID().toString())
			.set(dataset.name, datasetName)
			.set(dataset.sourceDatasetId, sourceDatasetId)
			.set(dataset.filterConditions, filterConditions)
			.executeWithKey(dataset.id);
		
		int index = 0;
		for(Column column : columnList) {
			insert(datasetColumn)
				.set(datasetColumn.datasetId, datasetId)
				.set(datasetColumn.index, index++)
				.set(datasetColumn.name, column.getName())
				.set(datasetColumn.dataType, column.getDataType().toString())
				.execute();
		}
	}
}
