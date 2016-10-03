package nl.idgis.publisher.monitor;

import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class XPathHelper {
	
	private final XPath xpath;
	
	private final SimpleNamespaceContext namespaceContext;
	
	public XPathHelper() {
		xpath = XPathFactory
				.newInstance()
				.newXPath();
			
		namespaceContext = new SimpleNamespaceContext();
		xpath.setNamespaceContext(namespaceContext);
	}
	
	public XPathHelper bindNamespaceUri(String prefix, String namespaceUri) {
		namespaceContext.bindNamespaceUri(prefix, namespaceUri);
		return this;
	}
	
	public String getString(Node node, String expression) {
		try {
			return 
				Optional
					.of((String)xpath.evaluate(expression, node, XPathConstants.STRING))
					.filter(s -> !s.trim().isEmpty())
					.orElse(null);
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Stream<Node> getNodes(Node node, String expression) {
		try {
			NodeList nodeList = (NodeList)xpath.evaluate(expression, node, XPathConstants.NODESET);
						
			return IntStream
				.range(0, nodeList.getLength())
				.mapToObj(nodeList::item);
		} catch(RuntimeException e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}