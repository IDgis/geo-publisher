package nl.idgis.publisher.harvester.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.metadata.messages.ParseMetadataDocument;
import nl.idgis.publisher.xml.messages.NotParseable;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;

public class MetadataDocumentTest {

	private static final FiniteDuration AWAIT_DURATION = Duration.create(15, TimeUnit.SECONDS);

	@Test
	public void testRead() throws Exception {
		InputStream stream = MetadataDocumentTest.class.getResourceAsStream("metadata.xml");
		assertNotNull("test metadata document not found", stream);
		
		byte[] content = IOUtils.toByteArray(stream);
		
		ActorSystem system = ActorSystem.create();
		
		ActorRef factory = system.actorOf(MetadataDocumentFactory.props());
		Future<Object> future = Patterns.ask(factory, new ParseMetadataDocument(content), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		assertTrue("didn't receive a MetadataDocument", result instanceof MetadataDocument);
		
		MetadataDocument document = (MetadataDocument)result;
		
		result = document.getTitle();		
		assertEquals("wrong title", "Zeer kwetsbare gebieden", result);
		
		result = document.getAlternateTitle();		
		assertEquals("wrong alternate title", "B4.wav_polygon (b4\\b46)", result);
		
		result = document.getRevisionDate();		
		assertTrue("wrong GetRivisionDate response type", result instanceof Date);
		
		Date date = (Date)result;
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		assertEquals(2009, calendar.get(Calendar.YEAR));
		assertEquals(Calendar.AUGUST, calendar.get(Calendar.MONTH));
		assertEquals(14, calendar.get(Calendar.DAY_OF_MONTH));
	}
	
	@Test
	public void testNotParseable() throws Exception {
		ActorSystem system = ActorSystem.create();
		
		ActorRef factory = system.actorOf(MetadataDocumentFactory.props());
		Future<Object> future = Patterns.ask(factory, new ParseMetadataDocument("Not valid metadata!".getBytes("utf-8")), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		
		assertTrue(result instanceof NotParseable);
		
	}
}
