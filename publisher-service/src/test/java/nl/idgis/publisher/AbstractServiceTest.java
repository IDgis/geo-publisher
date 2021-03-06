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
import nl.idgis.publisher.utils.TypedIterable;

public abstract class AbstractServiceTest extends AbstractDatabaseTest {
	
	protected ActorRef jobManager;
	
	protected ActorRef datasetManager;
	
	protected ActorRef serviceManager;
	@Before
	public void managers() {
		jobManager = actorOf(JobManager.props(database), "jobManager");
		datasetManager = actorOf(DatasetManager.props(database), "datasetManager");
		serviceManager = actorOf(ServiceManager.props(database, datasetManager), "serviceManager");
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
	
	protected void updateDataset(String datasetIdentification, String datasetName, 
			String sourceDatasetIdentification, List<Column> columnList, String filterConditions) {
		
		int sourceDatasetId = 
			query().from(sourceDataset)
			.where(sourceDataset.externalIdentification.eq(sourceDatasetIdentification))
			.singleResult(sourceDataset.id);
		
		int datasetId = 
			query().from(dataset)
			.where(dataset.identification.eq(datasetIdentification))
			.singleResult(dataset.id);
		
		update(dataset)
			.set(dataset.name, datasetName)
			.set(dataset.sourceDatasetId, sourceDatasetId)
			.set(dataset.filterConditions, filterConditions)
			.where(dataset.id.eq(datasetId))
			.execute();
		
		delete(datasetColumn)
			.where(datasetColumn.datasetId.eq(datasetId))
			.execute();
		
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
	
	protected void createDataset(String datasetIdentification, String datasetName, 
		String sourceDatasetIdentification, List<Column> columnList, String filterConditions) {
		
		int sourceDatasetId = 
			query().from(sourceDataset)
			.where(sourceDataset.externalIdentification.eq(sourceDatasetIdentification))
			.singleResult(sourceDataset.id);
		
		int datasetId = insert(dataset)
			.set(dataset.identification, datasetIdentification)			
			.set(dataset.name, datasetName)
			.set(dataset.sourceDatasetId, sourceDatasetId)
			.set(dataset.filterConditions, filterConditions)
			.set(dataset.metadataIdentification, UUID.randomUUID().toString())
			.set(dataset.metadataFileIdentification, UUID.randomUUID().toString())
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
