package nl.idgis.publisher.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class XMLDocument {
	
	protected final Document document;
	
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
	
	/**
	 * Remove all nodes from a document given by an xpath expression. 
	 * @param relevant namespaces for the nodes in the document
	 * @param xpath expression that points to the node(s) to be removed
	 * @return nr of removed nodes
	 * @throws NotFound 
	 */
	public int removeNodes(BiMap<String, String> namespaces, String path) throws NotFound {
		try {
			NodeList nodeList = (NodeList)getXPath(namespaces).evaluate(path, document, XPathConstants.NODESET);
			
			int nrOfNodes = nodeList.getLength();
			for (int i = 0; i < nrOfNodes; i++) {
				Node node = nodeList.item(i);
				node.getParentNode().removeChild(node);
			} 
			
			return nrOfNodes;
		} catch(Exception e) {
			throw new NotFound(namespaces, path, e);
		}
	}
	
	public String addNode(BiMap<String, String> namespaces, String parentPath, String[] followingSiblings, String name) throws NotFound {
		return addNode(namespaces, parentPath, followingSiblings, name, null, null);	
	}
	
	public String addNode(BiMap<String, String> namespaces, String parentPath, String name,  String content, Map<String, String> attributes) throws NotFound {
		return addNode(namespaces, parentPath, null, name, content, attributes);
	}
	
	public String addNode(BiMap<String, String> namespaces, String parentPath, String name) throws NotFound {
		return addNode(namespaces, parentPath, null, name, null, null);
	}
	
	public String addNode(BiMap<String, String> namespaces, String parentPath, String name,  String content) throws NotFound {
		return addNode(namespaces, parentPath, null, name, content, null);
	}
	
	public String addNode(BiMap<String, String> namespaces, String parentPath, String[] followingSiblings, String name,  String content) throws NotFound {
		return addNode(namespaces, parentPath, followingSiblings, name, content,  null);	
	}
	
	public String addNode(BiMap<String, String> namespaces, String parentPath, String[] followingSiblings, String name,  Map<String, String> attributes) throws NotFound {
		return addNode(namespaces, parentPath, followingSiblings, name, null, attributes);
	}
	
	public String addNode(BiMap<String, String> namespaces, String parentPath, String name,  Map<String, String> attributes) throws NotFound {
		return addNode(namespaces, parentPath, null, name, null, attributes);
	}
	
	protected static QName toQName(BiMap<String, String> namespaces, String name) {
		if(name.contains(":")) {
			String[] nameParts = name.split(":");
			
			if(namespaces.containsKey(nameParts[0])) {
				return new QName(namespaces.get(nameParts[0]), nameParts[1]);
			} else {
				throw new IllegalArgumentException("Unmapped prefix: " + nameParts[0]);
			}
		} else {
			return new QName(name);
		}
	}
	
	protected static QName[] toQNames(BiMap<String, String> namespaces, String... names) {
		List<QName> retval = new ArrayList<>();
		
		for(String name : names) {
			retval.add(toQName(namespaces, name));
		}
		
		return retval.toArray(new QName[retval.size()]);
	}
	
	protected Element createElement(Node context, BiMap<String, String> namespaces, String name, String content, Map<String, String> attributes) {
		if(name.contains("/")) {
			int separatorIndex = name.indexOf("/");
			
			Element newElement = createElement(context, namespaces, name.substring(0, separatorIndex), null, null);
			newElement.appendChild(createElement(context, namespaces, name.substring(separatorIndex + 1), content, attributes));
			
			return newElement;
		} else {
			QName qName = toQName(namespaces, name);
			
			Element newElement = document.createElementNS(qName.getNamespaceURI(), getQualifiedName(context, qName));
			
			if(content != null) {
				newElement.appendChild(document.createTextNode(content));
			}		
			
			if(attributes != null) {
				for(Map.Entry<String, String> attribute : attributes.entrySet()) {
					QName attributeName = toQName(namespaces, attribute.getKey());
					
					newElement.setAttributeNS(attributeName.getNamespaceURI(), getQualifiedName(context, attributeName), attribute.getValue());
				}
			}
			
			return newElement;
		}
	}

	private static String getQualifiedName(Node context, QName qName) {
		String prefix = context.lookupPrefix(qName.getNamespaceURI());
		
		if(prefix == null) {
			return qName.getLocalPart();
		} else {
			return prefix + ":" + qName.getLocalPart();
		}
	}
	
	public String addNode(BiMap<String, String> namespaces, String parentPath, String[] followingSiblings, String name, String content, Map<String, String> attributes) throws NotFound {
		XPath xpath = getXPath(namespaces);
		
		try {
			Node parentNode = (Node)xpath.evaluate(parentPath, document, XPathConstants.NODE);
			
			Element newElement = createElement(parentNode, namespaces, name, content, attributes);
			QName newElementName = new QName(newElement.getNamespaceURI(), newElement.getLocalName());
			
			int sameElementCount = 1;
			
			Set<QName> followingSiblingsSet = new HashSet<QName>();
			if(followingSiblings != null) {
				for(String followingSibling : followingSiblings) {
					followingSiblingsSet.add(toQName(namespaces, followingSibling));
				}
			}
			
			NodeList children = parentNode.getChildNodes();
			
			for(int i = 0; i < children.getLength(); i++) {
				Node childNode = children.item(i);
				if(childNode.getNodeType() == Node.ELEMENT_NODE) {
					QName childName = new QName(childNode.getNamespaceURI(), childNode.getLocalName());
					
					if(followingSiblingsSet.contains(childName)) {
						parentNode.insertBefore(newElement, childNode);
						
						return getResultXPath(parentPath, name, sameElementCount);
					}
					
					if(newElementName.equals(childName)) {
						sameElementCount++;
					}
				}
			}			
			
			parentNode.appendChild(newElement);
			
			return getResultXPath(parentPath, name, sameElementCount);			 
		} catch(Exception e) {
			throw new NotFound(namespaces, parentPath, e);
		}
	}

	private String getResultXPath(String parentPath, String name, int sameElementCount) {
		if(name.contains("/")) {
			int separatorIndex = name.indexOf("/");
			
			return parentPath + "/" + name.substring(0, separatorIndex) 
					+ "[" + sameElementCount + "]/"
					+ name.substring(separatorIndex + 1);
		} else {
			return parentPath + "/" + name + "[" + sameElementCount + "]";
		}
	}

	public String toString() {
		try {
			Transformer tf = TransformerFactory.newInstance().newTransformer();
			tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			tf.setOutputProperty(OutputKeys.INDENT, "yes");
			Writer out = new StringWriter();
			tf.transform(new DOMSource(document), new StreamResult(out));

			return out.toString();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}	
	
	@Override
	public XMLDocument clone() {
		try {
			TransformerFactory tfactory = TransformerFactory.newInstance();
			Transformer tx   = tfactory.newTransformer();
			
			DOMSource source = new DOMSource(document);
			DOMResult result = new DOMResult();
			
			tx.transform(source,result);
			return new XMLDocument((Document)result.getNode());
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void removeStylesheet() {
		NodeList children = document.getChildNodes();
		for(int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if(n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
				ProcessingInstruction pi = (ProcessingInstruction)n;
				if("xml-stylesheet".equals(pi.getTarget())) {
					document.removeChild(pi);
				}
			}
		}
	}

	public void setStylesheet(String stylesheet) {
		String data = "type=\"text/xsl\" href=\"" + stylesheet + "\"";
		
		NodeList children = document.getChildNodes();
		for(int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if(n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
				ProcessingInstruction pi = (ProcessingInstruction)n;
				if("xml-stylesheet".equals(pi.getTarget())) {
					pi.setData(data);
					return;
				}
			}
		}
		
		document.insertBefore(
				document.createProcessingInstruction("xml-stylesheet", data),
				children.item(0));
	}
}
