package nl.idgis.publisher.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.OutputKeys;
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

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
	
	/**
	 * Get the node list for a certain xpath in a document.
	 * @param namespaces
	 * @param path
	 * @return nodeList
	 */
	public NodeList getNodeList(BiMap<String, String> namespaces, String path) {
		NodeList nodeList = null;
		try {
			nodeList = (NodeList)getXPath(namespaces).evaluate(path, document, XPathConstants.NODESET);
		} catch(Exception e) {
			
		}
		return nodeList;		
	}

	/**
	 * Get the node for a certain xpath in a document.
	 * @param namespaces
	 * @param path
	 * @return nodeList
	 */
	public Node getNode(BiMap<String, String> namespaces, String path) {
		Node node = null;
		try {
			node = (Node)getXPath(namespaces).evaluate(path, document, XPathConstants.NODE);
		} catch(Exception e) {
			
		}
		return node;		
	}

	/**
	 * Remove all nodes from a document given by an xpath expression. 
	 * @param relevant namespaces for the nodes in the document
	 * @param xpath expression that points to the node(s) to be removed
	 * @return nr of removed nodes
	 */
	public int removeNodes(BiMap<String, String> namespaces, String path) {
		NodeList nodeList= getNodeList(namespaces, path);
		int nrOfNodes = nodeList.getLength();
		for (int i = 0; i < nrOfNodes; i++) {
			Node node = nodeList.item(i);
			Node removedNode = node.getParentNode().removeChild(node);
//			System.out.println("#"+(i+1)+" node removed: "+removedNode.getNodeName());
		} 
		return nrOfNodes;
	}
	
	/**
	 * Add a Node with the proper namespace and optional text content.
	 * @param parentNode the new node will be a child of parentNode 
	 * @param namespaceUri e.g. "http://www.isotc211.org/2005/gmd"
	 * @param nodeName e.g. "gmd:name"
	 * @param nodeContent text content of the node or null if this node will have further children
	 * @return the node added
	 */
	public Node addNode(Node parentNode, String namespaceUri, String nodeName,  String nodeContent){
        Node node ;
        if (nodeContent != null){
        	node = document.createElement(nodeName);
        	node.appendChild(document.createTextNode(nodeContent));
        }else{
        	node = document.createElementNS(namespaceUri, nodeName);
        }
        parentNode.appendChild(node);
        return node;
	}

	/**
	 * Add a Node with the proper namespace and optional attributes.
	 * @param parentNode the new node will be a child of parentNode 
	 * @param namespaceUri e.g. "http://www.isotc211.org/2005/gmd"
	 * @param nodeName e.g. "gmd:name"
	 * @param attributes in string array [attr1.name, attr1.value, attr2.name, attr2.value, ...]
	 * @return the node added
	 */
	public Node addNodeWithAttributes(Node parentNode, String namespaceUri, String nodeName,  String[] attributes){
        Element elem ;
        elem = document.createElementNS(namespaceUri, nodeName);
        if (attributes.length > 0){
        	for (int i = 0; i < attributes.length; i+=2) {				
        		elem.setAttribute(attributes[i],  attributes[i+1]);
        	} 
        }
        parentNode.appendChild(elem);
        return elem;
	}

	public static final void prettyPrint(Node xml) throws Exception {
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		Writer out = new StringWriter();
		tf.transform(new DOMSource(xml), new StreamResult(out));
		System.out.println(out.toString());
	}

	
}
