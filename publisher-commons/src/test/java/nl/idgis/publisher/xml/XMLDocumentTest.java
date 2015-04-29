package nl.idgis.publisher.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import nl.idgis.publisher.xml.exceptions.NotParseable;

import org.junit.Test;
import org.w3c.dom.Node;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class XMLDocumentTest {

	@Test
	public void testParsing() throws Exception {
		XMLDocumentFactory factory = new XMLDocumentFactory();
		
		byte[] content = "<a xmlns='aURI'><b xmlns='bURI'>Hello</b><c><d>World!</d></c></a>".getBytes("utf-8");
		XMLDocument document = factory.parseDocument(content);
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		namespaces.put("b", "bURI");
		
		String result = document.getString(namespaces, "/a:a/b:b");
		assertEquals("Hello", result);
	}
	
	@Test(expected=NotParseable.class)
	public void testUnparseable() throws Exception {
		XMLDocumentFactory factory = new XMLDocumentFactory();
		
		factory.parseDocument("This is not XML!".getBytes("utf-8"));
	}
	
	@Test
	public void testUpdateString() throws Exception{
		XMLDocumentFactory factory = new XMLDocumentFactory();
		
		byte[] content = "<a xmlns='aURI'><b xmlns='bURI'>Hello</b><c><d>World!</d></c></a>".getBytes("utf-8");		
		
		XMLDocument document = factory.parseDocument(content);
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		namespaces.put("b", "bURI");
		
		String result = document.getString(namespaces, "/a:a/b:b");		
		assertEquals("Hello", result);
		
		document.updateString(namespaces, "/a:a/b:b", "New Value");
		
		result = document.getString(namespaces, "/a:a/b:b");				
		assertEquals("New Value", result);
	}
	
	@Test
	public void testGetContent() throws Exception {
		XMLDocumentFactory factory = new XMLDocumentFactory();
		
		byte[] content = "<a xmlns='aURI'><b xmlns='bURI'>Hello</b><c><d>World!</d></c></a>".getBytes("utf-8");
		
		XMLDocument document = factory.parseDocument(content);
		
		document.getContent();		
	}
	
	@Test
	public void testAddNode() throws Exception {
		XMLDocumentFactory factory = new XMLDocumentFactory();
		
		byte[] content = "<a:a xmlns:a='aURI'><a:b/><a:c/><a:d/></a:a>".getBytes("utf-8");
		
		XMLDocument document = factory.parseDocument(content);
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		
		String resultPath = document.addNode(namespaces, "/a:a", "a:e", "Hello world!");
		assertEquals("/a:a/a:e[1]", resultPath);
		
		resultPath = document.addNode(namespaces, "/a:a", "a:e", "Hello world(2)!");
		assertEquals("/a:a/a:e[2]", resultPath);
		
		document.addNode(namespaces, "/a:a/a:e[1]", "a:k", "SomeText");
		assertEquals("SomeText", document.getString(namespaces, "/a:a/a:e/a:k"));
		
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
		XMLDocumentFactory factory = new XMLDocumentFactory();
		
		byte[] content = "<a xmlns='aURI'><b/><c/><d/></a>".getBytes("utf-8");
		
		XMLDocument document = factory.parseDocument(content);
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		
		Node node = document.createElement(document.document, namespaces, "a:a/a:b/a:c", "text", Collections.<String, String>emptyMap());
		assertEquals("a", node.getLocalName());
		
		node = node.getFirstChild();
		assertEquals("b", node.getLocalName());
		
		node = node.getFirstChild();
		assertEquals("c", node.getLocalName());
		
		assertEquals("text", node.getFirstChild().getTextContent());
	}
	
	@Test
	public void testClone() throws Exception {
		XMLDocumentFactory factory = new XMLDocumentFactory();
		
		byte[] content = "<a xmlns='aURI'/>".getBytes("utf-8");	
		
		XMLDocument document = factory.parseDocument(content);
		
		XMLDocument clonedDocument = document.clone();
		assertNotNull(clonedDocument);
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("a", "aURI");
		
		clonedDocument.addNode(namespaces, "/a:a", "a:b", "Hello world!");
		assertEquals("Hello world!", clonedDocument.getString(namespaces, "/a:a/a:b"));
		
		try {
			document.getString(namespaces, "/a:a/a:b");
			fail();
		} catch(Exception e) {}
	}
	
	@Test
	public void testRemoveStylesheet() throws Exception {
		XMLDocumentFactory factory = new XMLDocumentFactory();
		
		String content = 
			"<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>"
			+ "<?xml-stylesheet type=\"text/xsl\" href=\"stylesheet.xsl\"?>"
			+ "<document/>";
		
		XMLDocument document = factory.parseDocument(content.getBytes("utf-8"));

		document.removeStylesheet();
		
		String newContent = new String(document.getContent(), "utf-8");
		assertFalse(newContent.contains("stylesheet.xsl"));
	}
}
