package nl.idgis.publisher.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import nl.idgis.publisher.utils.XMLUtils;
import nl.idgis.publisher.utils.XMLUtils.XPathHelper;
import nl.idgis.publisher.xml.exceptions.MultipleNodes;
import nl.idgis.publisher.xml.exceptions.NotFound;
import nl.idgis.publisher.xml.exceptions.NotTextOnly;
import nl.idgis.publisher.xml.exceptions.QueryFailure;

public class XMLDocument {
	
	protected final Document document;
			
	public XMLDocument(Document document) {
		this.document = document;
	}
	
	public XPathHelper xpath(Optional<Map<String, String>> optionalNamespaces) {
		return XMLUtils.xpath(document, optionalNamespaces);
	}
	
	public String getString(String path) throws NotFound {
		return getString(HashBiMap.<String, String>create(), path);
	}
	
	public Node getNode(BiMap<String, String> namespaces, String path) throws NotFound {
		return xpath(Optional.of(namespaces))
				.node(path)
				.flatMap(node -> Optional.of(node.getItem()))
				.orElseThrow(() -> new NotFound(namespaces, path));
	}
	
	public List<Node> getNodes(BiMap<String, String> namespaces, String path) throws NotFound {
		return xpath(Optional.of(namespaces))
				.nodes(path)
				.stream()
				.map(node -> node.getItem())
				.collect(Collectors.toList());
	}
	
	public String getString(BiMap<String, String> namespaces, String path) throws NotFound {
		return xpath(Optional.of(namespaces))
			.string(path)
			.orElseThrow(() -> new NotFound(namespaces, path));
	}	
	
	public void updateString(BiMap<String, String> namespaces, String expression, String newValue) throws QueryFailure {
		Iterator<XPathHelper> i = 
			xpath(Optional.of(namespaces))
				.nodes(expression).iterator();
		
		if(i.hasNext()) {
			XPathHelper item = i.next();
			
			if(i.hasNext()) {
				throw new MultipleNodes(namespaces, expression);
			}
			
			if(item.isTextOnly()) {
				item.setTextContent(newValue);
			} else {
				throw new NotTextOnly(namespaces, expression);
			}
		} else {
			throw new NotFound(namespaces, expression);
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
		List<XPathHelper> nodes = xpath(Optional.of(namespaces)).nodes(path);		
		nodes.forEach(XPathHelper::remove);
		return nodes.size();
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
	
	public String addNode(BiMap<String, String> namespaces, String parentPath, String[] followingSiblings, String name, String content, Map<String, String> attributes) throws NotFound {
		XPathHelper parent = xpath(Optional.of(namespaces))
			.node(parentPath)
			.orElseThrow(() -> new NotFound(namespaces, parentPath));
		
		return parent.createElement(name, content, attributes, followingSiblings);
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
	
	private XMLDocument clone(Node root) {
		try {
			TransformerFactory tfactory = TransformerFactory.newInstance();
			Transformer tx   = tfactory.newTransformer();
			
			DOMSource source = new DOMSource(root);
			DOMResult result = new DOMResult();
			
			tx.transform(source,result);
			return new XMLDocument((Document)result.getNode());
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public XMLDocument clone(BiMap<String, String> namespaces, String path) {
		return clone(
			xpath(Optional.ofNullable(namespaces))
				.node(path)
				.orElseThrow(() -> new IllegalArgumentException("path not found: " + path))
				.getItem());
	}
	
	@Override
	public XMLDocument clone() {
		return clone(document);
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
