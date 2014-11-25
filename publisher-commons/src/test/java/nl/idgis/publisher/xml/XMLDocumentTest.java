package nl.idgis.publisher.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import nl.idgis.publisher.xml.messages.NotParseable;
import nl.idgis.publisher.xml.messages.ParseDocument;

import org.junit.Test;
import org.w3c.dom.Node;

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
		
		String resultPath = document.addNode(namespaces, "/a:a", "a:e", "Hello world!");
		assertEquals("/a:a/a:e[1]", resultPath);
		
		resultPath = document.addNode(namespaces, "/a:a", "a:e", "Hello world(2)!");
		assertEquals("/a:a/a:e[2]", resultPath);
		
		assertEquals("Hello world(2)!", document.getString(namespaces, resultPath));
		
		resultPath = document.addNode(namespaces, "/a:a", "a:e/a:j");
		assertEquals("/a:a/a:e[3]/a:j", resultPath);
		
		document.addNode(namespaces, "/a:a", new String[]{"a:e"}, "a:f");
		
		assertEquals("Hello world!", document.getString(namespaces, "/a:a/a:f/following-sibling::a:e[1]/text()"));
		
		Map<String, String> attributes = new HashMap<>();
		attributes.put("a:h", "42");
		document.addNode(namespaces, "/a:a", new String[]{"a:e"}, "a:g", attributes);
		
		assertEquals("42", document.getString(namespaces, "/a:a/a:f/following-sibling::a:g/@a:h"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testToQName() {
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		namespaces.put("b", "bURI");
		
		QName result = XMLDocument.toQName(namespaces, "noNamespaceURI");		
		assertEquals(new QName("noNamespaceURI"), result);
		
		result = XMLDocument.toQName(namespaces, "a:withNamespaceURI");		
		assertEquals(new QName("aURI", "withNamespaceURI"), result);
		
		XMLDocument.toQName(namespaces, "c:invalidPrefix");
	}
	
	@Test
	public void testToQNames() {
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		namespaces.put("b", "bURI");
		
		QName[] result = XMLDocument.toQNames(namespaces, "a:b", "b:a");
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals(new QName("aURI", "b"), result[0]);
		assertEquals(new QName("bURI", "a"), result[1]);
	}
	
	@Test
	public void createElement() throws Exception {
		ActorSystem system = ActorSystem.create();		
		
		ActorRef factory = system.actorOf(XMLDocumentFactory.props());
		
		byte[] content = "<a xmlns='aURI'><b/><c/><d/></a>".getBytes("utf-8");
		Future<Object> future = Patterns.ask(factory, new ParseDocument(content), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		assertTrue("didn't receive an XMLDocument", result instanceof XMLDocument);
		
		XMLDocument document = (XMLDocument)result;
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		
		Node node = document.createElement(namespaces, "a:a/a:b/a:c", "text", Collections.<String, String>emptyMap());
		assertEquals("a", node.getLocalName());
		
		node = node.getFirstChild();
		assertEquals("b", node.getLocalName());
		
		node = node.getFirstChild();
		assertEquals("c", node.getLocalName());
		
		assertEquals("text", node.getFirstChild().getTextContent());
	}
}
