package nl.idgis.publisher.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import nl.idgis.publisher.xml.messages.NotParseable;
import nl.idgis.publisher.xml.messages.ParseDocument;

import org.junit.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;

public class XMLDocumentTest {

	private static final FiniteDuration AWAIT_DURATION = Duration.create(15, TimeUnit.SECONDS);

	@Test
	public void testParsing() throws Exception {
		ActorSystem system = ActorSystem.create();		
		
		ActorRef factory = system.actorOf(XMLDocumentFactory.props());
		
		byte[] content = "<a xmlns='aURI'><b xmlns='bURI'>Hello</b><c><d>World!</d></c></a>".getBytes("utf-8");
		Future<Object> future = Patterns.ask(factory, new ParseDocument(content), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		assertTrue("didn't receive an XMLDocument", result instanceof XMLDocument);
		
		XMLDocument document = (XMLDocument)result;
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		namespaces.put("b", "bURI");
		
		result = document.getString(namespaces, "/a:a/b:b");
		assertEquals("Hello", result);
	}
	
	@Test
	public void testUnparseable() throws Exception {
		ActorSystem system = ActorSystem.create();		
		
		ActorRef factory = system.actorOf(XMLDocumentFactory.props());
		
		Future<Object> future = Patterns.ask(factory, new ParseDocument("This is not XML!".getBytes("utf-8")), 15000);
		
		Object response = Await.result(future, AWAIT_DURATION);
		assertTrue(response instanceof NotParseable);
		
		// test if the factory is still operational
		future = Patterns.ask(factory, new ParseDocument("<tag/>".getBytes("utf-8")), 15000);
		
		response = Await.result(future, AWAIT_DURATION);
		assertTrue(response instanceof XMLDocument);
	}
	
	@Test
	public void testUpdateString() throws Exception{
		ActorSystem system = ActorSystem.create();		
		
		ActorRef factory = system.actorOf(XMLDocumentFactory.props());
		
		byte[] content = "<a xmlns='aURI'><b xmlns='bURI'>Hello</b><c><d>World!</d></c></a>".getBytes("utf-8");
		Future<Object> future = Patterns.ask(factory, new ParseDocument(content), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		assertTrue("didn't receive an XMLDocument", result instanceof XMLDocument);
		
		XMLDocument document = (XMLDocument)result;
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		namespaces.put("b", "bURI");
		
		result = document.getString(namespaces, "/a:a/b:b");		
		assertEquals("Hello", result);
		
		document.updateString(namespaces, "/a:a/b:b", "New Value");
		
		result = document.getString(namespaces, "/a:a/b:b");				
		assertEquals("New Value", result);
	}
	
	@Test
	public void testGetContent() throws Exception {
		ActorSystem system = ActorSystem.create();		
		
		ActorRef factory = system.actorOf(XMLDocumentFactory.props());
		
		byte[] content = "<a xmlns='aURI'><b xmlns='bURI'>Hello</b><c><d>World!</d></c></a>".getBytes("utf-8");
		Future<Object> future = Patterns.ask(factory, new ParseDocument(content), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		assertTrue("didn't receive an XMLDocument", result instanceof XMLDocument);
		
		XMLDocument document = (XMLDocument)result;
		result = document.getContent();
		assertTrue(result instanceof byte[]);
	}
	
	@Test
	public void testAddNode() throws Exception {
		ActorSystem system = ActorSystem.create();		
		
		ActorRef factory = system.actorOf(XMLDocumentFactory.props());
		
		byte[] content = "<a xmlns='aURI'><b/><c/><d/></a>".getBytes("utf-8");
		Future<Object> future = Patterns.ask(factory, new ParseDocument(content), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		assertTrue("didn't receive an XMLDocument", result instanceof XMLDocument);
		
		XMLDocument document = (XMLDocument)result;
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		
		document.addNode(namespaces, "/a:a", new QName("aURI", "e"), "Hello world!");
		
		assertEquals("Hello world!", document.getString(namespaces, "/a:a/a:e/text()"));
		
		document.addNode(namespaces, "/a:a", Arrays.asList(new QName("aURI", "e")), new QName("aURI", "f"));
		
		assertEquals("Hello world!", document.getString(namespaces, "/a:a/a:f/following-sibling::a:e/text()"));
		
		Map<QName, String> attributes = new HashMap<>();
		attributes.put(new QName("aURI", "h"), "42");
		document.addNode(namespaces, "/a:a", Arrays.asList(new QName("aURI", "e")), new QName("aURI", "g"), attributes);
		
		assertEquals("42", document.getString(namespaces, "/a:a/a:f/following-sibling::a:g/@a:h"));
	}
}
