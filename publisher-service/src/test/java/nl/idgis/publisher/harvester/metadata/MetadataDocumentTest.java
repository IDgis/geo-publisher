package nl.idgis.publisher.harvester.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.metadata.messages.ParseMetadataDocument;
import nl.idgis.publisher.xml.messages.NotParseable;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Node;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;

public class MetadataDocumentTest {

	private static final FiniteDuration AWAIT_DURATION = Duration.create(15, TimeUnit.SECONDS);

	/**
	 * Get a MetadataDocument from test resources.
	 * @param name
	 * @return
	 * @throws Exception
	 */
	private MetadataDocument getDocument(String name) throws Exception {
		InputStream stream = MetadataDocumentTest.class.getResourceAsStream(name);
		assertNotNull("test metadata document not found", stream);
		
		byte[] content = IOUtils.toByteArray(stream);
		
		ActorSystem system = ActorSystem.create();
		
		ActorRef factory = system.actorOf(MetadataDocumentFactory.props());
		Future<Object> future = Patterns.ask(factory, new ParseMetadataDocument(content), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		assertTrue("didn't receive a MetadataDocument", result instanceof MetadataDocument);
		
		MetadataDocument document = (MetadataDocument)result;
		
		return document;
	}
	
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
	
	/*
	 * Service Linkage
	 */
	
	@Test
	public void testServiceLinkage() throws Exception{
		MetadataDocument document = getDocument("metadata.xml");

		// get gmd:MD_DigitalTransferOptions content
		String result = document.getDigitalTransferOptions();		
		assertTrue("No WMS link found", result.indexOf("OGC:WMS") > -1);

		// remove all gmd:online child nodes
		int i = document.removeServiceLinkages();
		assertTrue("There should be two removed linkages", i==2);
		
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		result = document.getDigitalTransferOptions();		
		assertTrue("Still WMS link found", result.indexOf("OGC:WMS") == -1);
		
		// add new gmd:online childnode
		document.setServiceLinkage("linkage", "protocol", "name");
		result = document.getDigitalTransferOptions();		
		assertTrue("No protocol found", result.indexOf("protocol") > -1);
		
		// remove all gmd:online child nodes		
		i = document.removeServiceLinkages();
		assertTrue("There should be one removed linkage", i==1);
		
		// check gmd:MD_DigitalTransferOptions has no gmd:online child node anymore
		result = document.getDigitalTransferOptions();		
		assertTrue("Unexpected protocol found", result.indexOf("protocol") == -1);
	}
	
}
