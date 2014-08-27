package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QNotification.notification;
import static nl.idgis.publisher.database.QNotificationResult.notificationResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import nl.idgis.publisher.database.messages.AddNotification;
import nl.idgis.publisher.database.messages.AddNotificationResult;
import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.database.messages.RemoveNotification;
import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.protocol.messages.Ack;

import org.junit.Test;

import com.mysema.query.Tuple;

public class NotificationTest extends AbstractDatabaseTest {

	@Test
	public void testNotification() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "testDataSource")
			.set(dataSource.name, "My Test DataSource")
			.execute();
		
		Dataset dataset = createTestDataset();
		ask(new RegisterSourceDataset("testDataSource", dataset));
		
		Table table = dataset.getTable();
		ask(new CreateDataset("testDataset", "My Test Dataset", dataset.getId(), table.getColumns(), ""));
		
		ask(new CreateImportJob("testDataset"));
		
		List<ImportJobInfo> jobInfos = (List<ImportJobInfo>)ask(new GetImportJobs());
		assertNotNull(jobInfos);
		assertEquals(1, jobInfos.size());
		
		ImportJobInfo jobInfo = (ImportJobInfo)jobInfos.get(0);
		assertNotNull(jobInfo);		
		assertEquals("testDataset", jobInfo.getDatasetId());
		
		List<Notification> notifications = jobInfo.getNotifications();
		assertNotNull(notifications);
		assertTrue(notifications.isEmpty());
		
		ask(new AddNotification(jobInfo, ImportNotificationType.SOURCE_COLUMNS_CHANGED));
		
		Tuple notificationTuple =  
			query().from(notification)
			.singleResult(notification.all());
		
		assertNotNull(notificationTuple);
		assertEquals(ImportNotificationType.SOURCE_COLUMNS_CHANGED.name(), notificationTuple.get(notification.type));
		
		jobInfos = (List<ImportJobInfo>)ask(new GetImportJobs());
		assertNotNull(jobInfos);
		assertEquals(1, jobInfos.size());
		
		jobInfo = (ImportJobInfo)jobInfos.get(0);
		assertNotNull(jobInfo);		
		assertEquals("testDataset", jobInfo.getDatasetId());
		
		notifications = jobInfo.getNotifications();
		assertNotNull(notifications);
		assertEquals(1, notifications.size());
		
		Notification n = notifications.get(0);
		assertNotNull(n);		
		assertEquals(ImportNotificationType.SOURCE_COLUMNS_CHANGED, n.getType());
		assertNull(n.getResult());
		
		ask(new AddNotificationResult(
				jobInfo, 
				ImportNotificationType.SOURCE_COLUMNS_CHANGED, 
				ConfirmNotificationResult.OK));
		
		Tuple notificationResultTuple = 
				query().from(notificationResult)				
				.singleResult(notificationResult.all());
		
		assertNotNull(notificationResultTuple);
		assertEquals(
			notificationTuple.get(notification.id),
			notificationResultTuple.get(notificationResult.notificationId));
		assertEquals(
			ConfirmNotificationResult.OK.name(),
			notificationResultTuple.get(notificationResult.result));
		
		assertNotNull(
			query().from(notification)
				.leftJoin(notificationResult).on(notificationResult.notificationId.eq(notification.id))
				.singleResult(notificationResult.result));
		
		jobInfos = (List<ImportJobInfo>)ask(new GetImportJobs());
		assertNotNull(jobInfos);
		assertEquals(1, jobInfos.size());
		
		jobInfo = (ImportJobInfo)jobInfos.get(0);
		assertNotNull(jobInfo);		
		assertEquals("testDataset", jobInfo.getDatasetId());
		
		notifications = jobInfo.getNotifications();
		assertNotNull(notifications);
		assertEquals(1, notifications.size());
		
		n = notifications.get(0);
		assertNotNull(n);		
		assertEquals(ImportNotificationType.SOURCE_COLUMNS_CHANGED, n.getType());
		assertEquals(ConfirmNotificationResult.OK, n.getResult());
		
		ask(new RemoveNotification(jobInfo, ImportNotificationType.SOURCE_COLUMNS_CHANGED));
		
		assertFalse(
			query().from(notification)
				.where(notification.type.eq(ImportNotificationType.SOURCE_COLUMNS_CHANGED.name()))
				.exists());
		
		jobInfos = (List<ImportJobInfo>)ask(new GetImportJobs());
		assertNotNull(jobInfos);
		assertEquals(1, jobInfos.size());
		
		jobInfo = (ImportJobInfo)jobInfos.get(0);
		assertNotNull(jobInfo);		
		assertEquals("testDataset", jobInfo.getDatasetId());
		
		notifications = jobInfo.getNotifications();
		assertNotNull(notifications);
		assertTrue(notifications.isEmpty());
	}
}
