package nl.idgis.publisher.dataset;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QNotification.notification;
import static nl.idgis.publisher.database.QNotificationResult.notificationResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.database.messages.AddNotification;
import nl.idgis.publisher.database.messages.AddNotificationResult;
import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.RemoveNotification;

import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;

import nl.idgis.publisher.utils.TypedIterable;

import org.junit.Test;

import com.mysema.query.Tuple;

import static nl.idgis.publisher.utils.TestPatterns.ask;
import static nl.idgis.publisher.utils.TestPatterns.askAssert;

public class NotificationTest extends AbstractServiceTest {

	@Test
	public void testNotification() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "testDataSource")
			.set(dataSource.name, "My Test DataSource")
			.execute();
		
		Dataset dataset = createTestDataset();
		ask(database, new RegisterSourceDataset("testDataSource", dataset));
		
		Table table = dataset.getTable();
		ask(database, new CreateDataset("testDataset", "My Test Dataset", dataset.getId(), table.getColumns(), ""));
		
		ask(jobManager, new CreateImportJob("testDataset"));
		
		TypedIterable<?> jobInfos = askAssert(jobManager, new GetImportJobs(), TypedIterable.class);		
		assertTrue(jobInfos.contains(ImportJobInfo.class));		
		
		Iterator<ImportJobInfo> jobInfoItr = jobInfos.cast(ImportJobInfo.class).iterator();		
		assertTrue(jobInfoItr.hasNext());
		ImportJobInfo jobInfo = jobInfoItr.next();;
		
		assertNotNull(jobInfo);		
		assertEquals("testDataset", jobInfo.getDatasetId());
		
		assertFalse(jobInfoItr.hasNext());
		
		List<Notification> notifications = jobInfo.getNotifications();
		assertNotNull(notifications);
		assertTrue(notifications.isEmpty());
		
		ask(database, new AddNotification(jobInfo, ImportNotificationType.SOURCE_COLUMNS_CHANGED));
		
		Tuple notificationTuple =  
			query().from(notification)
			.singleResult(notification.all());
		
		assertNotNull(notificationTuple);
		assertEquals(ImportNotificationType.SOURCE_COLUMNS_CHANGED.name(), notificationTuple.get(notification.type));
		
		jobInfos = askAssert(jobManager, new GetImportJobs(), TypedIterable.class);		
		assertTrue(jobInfos.contains(ImportJobInfo.class));		
		
		jobInfoItr = jobInfos.cast(ImportJobInfo.class).iterator();		
		assertTrue(jobInfoItr.hasNext());
		jobInfo = jobInfoItr.next();
		
		assertNotNull(jobInfo);		
		assertEquals("testDataset", jobInfo.getDatasetId());
		
		notifications = jobInfo.getNotifications();
		assertNotNull(notifications);
		assertEquals(1, notifications.size());
		
		Notification n = notifications.get(0);
		assertNotNull(n);		
		assertEquals(ImportNotificationType.SOURCE_COLUMNS_CHANGED, n.getType());
		assertNull(n.getResult());
		
		ask(database, new AddNotificationResult(
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
		
		jobInfos = askAssert(jobManager, new GetImportJobs(), TypedIterable.class);		
		assertTrue(jobInfos.contains(ImportJobInfo.class));		
		
		jobInfoItr = jobInfos.cast(ImportJobInfo.class).iterator();		
		assertTrue(jobInfoItr.hasNext());
		jobInfo = jobInfoItr.next();
		assertNotNull(jobInfo);		
		assertEquals("testDataset", jobInfo.getDatasetId());
		
		notifications = jobInfo.getNotifications();
		assertNotNull(notifications);
		assertEquals(1, notifications.size());
		
		n = notifications.get(0);
		assertNotNull(n);		
		assertEquals(ImportNotificationType.SOURCE_COLUMNS_CHANGED, n.getType());
		assertEquals(ConfirmNotificationResult.OK, n.getResult());
		
		ask(database, new RemoveNotification(jobInfo, ImportNotificationType.SOURCE_COLUMNS_CHANGED));
		
		assertFalse(
			query().from(notification)
				.where(notification.type.eq(ImportNotificationType.SOURCE_COLUMNS_CHANGED.name()))
				.exists());
		
		jobInfos = askAssert(jobManager, new GetImportJobs(), TypedIterable.class);		
		assertTrue(jobInfos.contains(ImportJobInfo.class));		
		
		jobInfoItr = jobInfos.cast(ImportJobInfo.class).iterator();		
		assertTrue(jobInfoItr.hasNext());
		jobInfo = jobInfoItr.next();
		assertNotNull(jobInfo);		
		assertEquals("testDataset", jobInfo.getDatasetId());
		
		notifications = jobInfo.getNotifications();
		assertNotNull(notifications);
		assertTrue(notifications.isEmpty());
	}
}
