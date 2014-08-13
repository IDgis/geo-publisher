package nl.idgis.publisher.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.xml.messages.GetString;
import nl.idgis.publisher.xml.messages.ParseDocument;

import org.junit.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;

public class XMLDocumentTest {

	@Test
	public void testParsing() throws Exception {
		ActorSystem system = ActorSystem.create();		
		
		ActorRef factory = system.actorOf(XMLDocumentFactory.props());
		
		byte[] content = "<a xmlns='aURI'><b xmlns='bURI'>Hello</b><c><d>World!</d></c></a>".getBytes("utf-8");
		Future<Object> future = Patterns.ask(factory, new ParseDocument(content), 15000);
		
		Object result = Await.result(future, Duration.create(15, TimeUnit.SECONDS));
		assertTrue("didn't receive an ActorRef", result instanceof ActorRef);
		
		ActorRef document = (ActorRef)result;
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		namespaces.put("b", "bURI");
		
		future = Patterns.ask(document, new GetString(namespaces, "/a:a/b:b"), 15000);
		result = Await.result(future, Duration.create(15, TimeUnit.SECONDS));		
		assertEquals("Hello", result);
	}
}
