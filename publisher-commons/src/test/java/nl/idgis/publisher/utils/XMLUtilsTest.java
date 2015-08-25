package nl.idgis.publisher.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.events.XMLEvent;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XMLUtilsTest {
	
	DocumentBuilder db;
	
	@Before
	public void builder() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		db = dbf.newDocumentBuilder();
	}
	
	private Document createTestDocument() {
		Document document = db.newDocument();
		
		Element root = document.createElementNS("test", "root");
		Element first = document.createElementNS("test", "first");
		Element second = document.createElementNS("test", "second");
		
		document.appendChild(root);
		root.appendChild(first);
		root.appendChild(second);
		
		first.appendChild(document.createTextNode("Hello"));
		first.appendChild(document.createTextNode(","));
		
		second.appendChild(document.createTextNode(" "));
		second.appendChild(document.createTextNode("world"));
		second.appendChild(document.createTextNode("!"));
		
		return document;
	}

	@Test
	public void testEquals() throws Exception {
		Document document = createTestDocument();
		assertTrue(XMLUtils.equals(document, document));
		
		Document anotherDocument = createTestDocument();
		assertTrue(XMLUtils.equals(document, anotherDocument));
		
		Node root = anotherDocument.getFirstChild();
		root.appendChild(anotherDocument.createElementNS("test", "third"));
		
		assertFalse(XMLUtils.equals(document, anotherDocument));
	}
	
	@Test
	public void testEqualsIgnoreWhitespace() throws Exception {
		Document a = createTestDocument();
		Document b = createTestDocument();
		
		assertTrue(XMLUtils.equalsIgnoreWhitespace(a, b));
		
		Node root = b.getFirstChild();
		assertEquals("Hello, world!", root.getTextContent());
		root.appendChild(b.createTextNode("\t\n"));
		
		Node first = b.getFirstChild().getFirstChild();
		assertEquals("Hello,", first.getTextContent());
		first.appendChild(b.createTextNode("\n\t\t"));
		
		Node second = first.getNextSibling();
		assertEquals(" world!", second.getTextContent());
		second.appendChild(b.createTextNode(" "));
		
		assertEquals("Hello,\n\t\t world! \t\n", root.getTextContent());
		
		assertFalse(XMLUtils.equals(a, b));		
		assertTrue(XMLUtils.equalsIgnoreWhitespace(a, b));
	}
	
	@Test
	public void testToEventList() throws Exception {
		List<XMLEvent> eventList = XMLUtils.toEventList(createTestDocument());
		assertNotNull(eventList);
		
		Iterator<XMLEvent> itr = eventList.iterator();
		assertNotNull(itr);
		
		assertTrue(itr.hasNext());
		
		XMLEvent startDocument = itr.next();
		assertNotNull(startDocument);
		assertTrue(startDocument.isStartDocument());
		
		assertTrue(itr.hasNext());
		
		XMLEvent startRoot = itr.next();
		assertNotNull(startRoot);
		assertTrue(startRoot.isStartElement());
		assertEquals(new QName("test", "root"), startRoot.asStartElement().getName());
		
		assertTrue(itr.hasNext());
		
		XMLEvent startFirst = itr.next();
		assertNotNull(startFirst);
		assertTrue(startFirst.isStartElement());
		assertEquals(new QName("test", "first"), startFirst.asStartElement().getName());
		
		assertTrue(itr.hasNext());
		
		XMLEvent firstText = itr.next();
		assertNotNull(firstText);
		assertTrue(firstText.isCharacters());
		assertEquals("Hello,", firstText.asCharacters().getData());
		
		assertTrue(itr.hasNext());
		
		XMLEvent endFirst = itr.next();
		assertNotNull(endFirst);
		assertTrue(endFirst.isEndElement());
		assertEquals(new QName("test", "first"), endFirst.asEndElement().getName());
		
		assertTrue(itr.hasNext());
		
		XMLEvent startSecond = itr.next();
		assertNotNull(startSecond);
		assertTrue(startSecond.isStartElement());
		assertEquals(new QName("test", "second"), startSecond.asStartElement().getName());
		
		assertTrue(itr.hasNext());
		
		XMLEvent secondText = itr.next();
		assertNotNull(secondText);
		assertTrue(secondText.isCharacters());
		assertEquals(" world!", secondText.asCharacters().getData());
		
		assertTrue(itr.hasNext());
		
		XMLEvent endSecond = itr.next();
		assertNotNull(endSecond);
		assertTrue(endSecond.isEndElement());
		assertEquals(new QName("test", "second"), endSecond.asEndElement().getName());
		
		assertTrue(itr.hasNext());
		
		XMLEvent endRoot = itr.next();
		assertNotNull(endRoot);
		assertTrue(endRoot.isEndElement());
		assertEquals(new QName("test", "root"), endRoot.asEndElement().getName());
		
		assertTrue(itr.hasNext());
		
		XMLEvent endDocument = itr.next();
		assertNotNull(endDocument);
		assertTrue(endDocument.isEndDocument());
		
		assertFalse(itr.hasNext());
	}
	
	@Test
	public void testXpath() {
		Document document = createTestDocument();
		
		assertTrue(XMLUtils.xpath(document).strings("/root/second").isEmpty());;
		
		Map<String, String> namespaces = new HashMap<>();
		namespaces.put("t", "test");
		assertFalse(XMLUtils.xpath(document, Optional.of(namespaces)).strings("/t:root/t:second").isEmpty());;
	}
}
