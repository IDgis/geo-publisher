package nl.idgis.publisher.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import nl.idgis.publisher.xml.exceptions.MultipleNodes;
import nl.idgis.publisher.xml.exceptions.NotFound;
import nl.idgis.publisher.xml.exceptions.NotTextOnly;
import nl.idgis.publisher.xml.exceptions.QueryFailure;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class XMLDocument {
	
	private final Document document;
	
	private XPathFactory xf;
	
	public XMLDocument(Document document) {
		this.document = document;
		
		xf = XPathFactory.newInstance();
	}
	
	private XPath getXPath(final BiMap<String, String> namespaces) {
		XPath xpath = xf.newXPath();
		xpath.setNamespaceContext(new NamespaceContext() {

			@Override
			public String getNamespaceURI(String prefix) {				
				return namespaces.get(prefix);
			}

			@Override
			public String getPrefix(String namespaceURI) {
				return namespaces.inverse().get(namespaceURI);
			}
			
			@Override
			@SuppressWarnings("rawtypes")
			public Iterator getPrefixes(String namespaceURI) {
				return Arrays.asList(getPrefix(namespaceURI)).iterator();
			}
			
		});
		
		return xpath;
	}
	
	public String getString(String path) throws NotFound {
		return getString(HashBiMap.<String, String>create(), path);
	}
	
	public String getString(BiMap<String, String> namespaces, String path) throws NotFound {
		try {
			String s = getXPath(namespaces).evaluate(path, document);
			if(s.isEmpty()) {
				throw new NotFound(namespaces, path);
			} else {
				return s;
			}
		} catch(Exception e) {
			throw new NotFound(namespaces, path, e);
		}
	}	

	private boolean isTextOnly(Node n) {
		NodeList children = n.getChildNodes();
		for(int i = 0; i < children.getLength(); i++) {
			if(children.item(i).getNodeType() != Node.TEXT_NODE) {
				return false;
			}
		}
		
		return true;
	}	
	
	public void updateString(BiMap<String, String> namespaces, String expression, String newValue) throws QueryFailure {
		NodeList nodeList;
		try {
			nodeList = (NodeList)getXPath(namespaces).evaluate(expression, document, XPathConstants.NODESET);
		} catch(Exception e) {
			throw new NotFound(namespaces, expression, e);
		}
		
		switch(nodeList.getLength()) {
			case 0:
				throw new NotFound(namespaces, expression);				
			case 1:
				Node n = nodeList.item(0);
				
				if(isTextOnly(n)) {
					n.setTextContent(newValue);
				} else {
					throw new NotTextOnly(namespaces, expression);
				}
				
				break;
			default:
				throw new MultipleNodes(namespaces, expression);
		}
		
	}

	public byte[] getContent() throws IOException {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t = tf.newTransformer();
			
			ByteArrayOutputStream boas = new ByteArrayOutputStream();
			t.transform(new DOMSource(document), new StreamResult(boas));
			boas.close();
			
			return boas.toByteArray();
		} catch(TransformerException e) {
			throw new IOException("Couldn't serialize xml", e);
		}
	}
}
