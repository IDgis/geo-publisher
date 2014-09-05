package nl.idgis.publisher.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.xml.messages.Close;
import nl.idgis.publisher.xml.messages.GetString;
import nl.idgis.publisher.xml.messages.NotParseable;
import nl.idgis.publisher.xml.messages.ParseDocument;
import nl.idgis.publisher.xml.messages.UpdateString;

import org.junit.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.AskTimeoutException;
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
		assertTrue("didn't receive an ActorRef", result instanceof ActorRef);
		
		ActorRef document = (ActorRef)result;
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		namespaces.put("b", "bURI");
		
		future = Patterns.ask(document, new GetString(namespaces, "/a:a/b:b"), 15000);
		result = Await.result(future, AWAIT_DURATION);		
		assertEquals("Hello", result);
	}
	
	@Test(expected=AskTimeoutException.class)	
	public void testClose() throws Exception {
		ActorSystem system = ActorSystem.create();		
		
		ActorRef factory = system.actorOf(XMLDocumentFactory.props());
		Future<Object> future = Patterns.ask(factory, new ParseDocument("<a/>".getBytes("utf-8")), 15000);

		ActorRef document = (ActorRef)Await.result(future, AWAIT_DURATION);
		future = Patterns.ask(document, new Close(), 15000);
		
		Object response = Await.result(future, AWAIT_DURATION);
		assertTrue(response instanceof Ack);
		
		// is not supposed to work anymore because we just closed the document
		future = Patterns.ask(document, new GetString(""), 1000);
		Await.result(future, AWAIT_DURATION);
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
		assertTrue(response instanceof ActorRef);
	}
	
	@Test
	public void testUpdateString() throws Exception{
		ActorSystem system = ActorSystem.create();		
		
		ActorRef factory = system.actorOf(XMLDocumentFactory.props());
		
		byte[] content = "<a xmlns='aURI'><b xmlns='bURI'>Hello</b><c><d>World!</d></c></a>".getBytes("utf-8");
		Future<Object> future = Patterns.ask(factory, new ParseDocument(content), 15000);
		
		Object result = Await.result(future, AWAIT_DURATION);
		assertTrue("didn't receive an ActorRef", result instanceof ActorRef);
		
		ActorRef document = (ActorRef)result;
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		namespaces.put("b", "bURI");
		
		future = Patterns.ask(document, new GetString(namespaces, "/a:a/b:b"), 15000);
		result = Await.result(future, AWAIT_DURATION);		
		assertEquals("Hello", result);
		
		future = Patterns.ask(document, new UpdateString(namespaces, "/a:a/b:b", "New Value"), 15000);
		result = Await.result(future, AWAIT_DURATION);
		assertTrue(result instanceof Ack);
		
		future = Patterns.ask(document, new GetString(namespaces, "/a:a/b:b"), 15000);
		result = Await.result(future, AWAIT_DURATION);		
		assertEquals("New Value", result);
	}
}
