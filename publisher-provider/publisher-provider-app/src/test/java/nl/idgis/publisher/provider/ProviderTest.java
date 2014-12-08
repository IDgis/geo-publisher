package nl.idgis.publisher.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.messages.Clear;
import nl.idgis.publisher.provider.messages.GetRecording;
import nl.idgis.publisher.provider.messages.Record;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.metadata.GetAllMetadata;
import nl.idgis.publisher.provider.protocol.metadata.PutMetadata;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.Ask;
import nl.idgis.publisher.utils.Ask.Response;

import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.util.Timeout;

public class ProviderTest {
	
	private static final FiniteDuration duration = FiniteDuration.create(10, TimeUnit.SECONDS);
	
	private static final Timeout timeout = new Timeout(duration);
	
	private ActorSystem actorSystem;
	
	private ActorRef sender, recorder, provider;
	
	private ActorSelection metadata, database;
	
	private MetadataDocument metadataDocument;
	
	@Before
	public void actors() {		
		actorSystem = ActorSystem.create("test");
		
		recorder = actorSystem.actorOf(Recorder.props(), "recorder");
		provider = actorSystem.actorOf(Provider.props(DatabaseMock.props(recorder), MetadataMock.props(recorder)), "provider");
		
		metadata = ActorSelection.apply(provider, "metadata");
		database = ActorSelection.apply(provider, "database");
	}
	
	@Before
	public void metadataDocument() throws Exception {
		InputStream inputStream = ProviderTest.class.getResourceAsStream("metadata.xml");
		assertNotNull(inputStream);
		
		byte[] content = IOUtils.toByteArray(inputStream);
		MetadataDocumentFactory metadataDocumentFactory = new MetadataDocumentFactory();
		metadataDocument = metadataDocumentFactory.parseDocument(content);
	}
	
	private <T> T ask(Object msg, Class<T> expected) throws Exception {
		return ask(provider, msg, expected);
	}
	
	private Response askResponse(ActorRef actorRef, Object msg) throws Exception {
		Response response = Await.result(Ask.askResponse(actorSystem, actorRef, msg, timeout), duration);
		sender = response.getSender();
		return response;
	}
	
	private Response askResponse(ActorSelection actorSelection, Object msg) throws Exception {
		Response response = Await.result(Ask.askResponse(actorSystem, actorSelection, msg, timeout), duration);
		sender = response.getSender();
		return response;
	}
	
	private <T> T ask(ActorRef actorRef, Object msg, Class<T> expected) throws Exception {
		return result(expected, askResponse(actorRef, msg).getMessage());
	}
	
	private <T> T ask(ActorSelection actorSelection, Object msg, Class<T> expected) throws Exception {
		return result(expected, askResponse(actorSelection, msg).getMessage());
	}

	private <T> T result(Class<T> expected, Object result) throws Exception {
		if(expected.isInstance(result)) {
			return expected.cast(result);
		} else {
			fail("expected: " + expected.getCanonicalName() + " received: " + result.getClass().getCanonicalName());
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	private Iterator<Record> playRecording() throws Exception {
		return ask(recorder, new GetRecording(), Iterable.class).iterator();
	}
	
	private void clearRecording() throws Exception {
		ask(recorder, new Clear(), Ack.class);
	}

	@Test
	public void testListDatasetInfo() throws Exception {
		Set<AttachmentType> attachmentTypes = new HashSet<>();
		attachmentTypes.add(AttachmentType.METADATA);
		
		ask(new ListDatasetInfo(attachmentTypes), End.class);
		
		Iterator<Record> recording = playRecording();
		assertTrue(recording.hasNext());
		
		Record record = recording.next();
		assertTrue(record.getMessage() instanceof GetAllMetadata);
		
		assertFalse(recording.hasNext());
		
		final String tableName = ProviderUtils.getTableName(metadataDocument.getAlternateTitle());
		ask(metadata, new PutMetadata("test", metadataDocument.getContent()), Ack.class);
		
		clearRecording();
		
		UnavailableDatasetInfo unavailableDatasetInfo = ask(new ListDatasetInfo(attachmentTypes), UnavailableDatasetInfo.class);
		assertEquals("test", unavailableDatasetInfo.getId());
		
		ask(sender, new NextItem(), End.class);
		
		recording = playRecording();
		assertTrue(recording.hasNext());
		
		record = recording.next();
		assertTrue(record.getMessage() instanceof GetAllMetadata);
		
		assertTrue(recording.hasNext());
		
		record = recording.next();
		Object message = record.getMessage(); 
		assertTrue(message instanceof DescribeTable);
		
		DescribeTable describeTable = (DescribeTable)message;
		assertEquals(tableName, describeTable.getTableName());
		
		assertTrue(recording.hasNext());
		
		record = recording.next();
		assertTrue(record.getMessage() instanceof PerformCount);
		
		assertFalse(recording.hasNext());
	}
}
