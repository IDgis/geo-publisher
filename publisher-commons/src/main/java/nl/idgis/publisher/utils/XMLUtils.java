package nl.idgis.publisher.utils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class XMLUtils {
	
	public static class XPathHelper {
	
		private final XPath xpath;
		
		private final Node item;
		
		private final String expression;
		
		private final BiMap<String, String> namespaces;
		
		private XPathHelper(XPath xpath, Node item, String expression, BiMap<String, String> namespaces) {
			this.xpath = xpath;
			this.item = item;
			this.expression = expression;
			this.namespaces = namespaces;
		}
		
		public Optional<String> getLocalName () {
			return Optional.ofNullable (item.getLocalName ());
		}
		
		public List<Integer> integers(String expression) {
			return stringsMap(expression, Integer::parseInt);
		}
	
		public List<String> strings(String expression) {
			return stringsMap(expression, Function.identity());
		}
		
		private <T> List<T> stringsMap(String expression, Function<? super String, ? extends T> mapper) {
			return flatMap(expression, node ->
				node.string()
					.map(mapper)
					.map(Stream::of)
					.orElse(Stream.empty()));
		}
		
		public <T> List<T> map(String expression, Function<? super XPathHelper, ? extends T> mapper) {
			return nodes(expression).stream()
				.map(mapper)
				.collect(Collectors.toList());
		}
		
		public <T> List<T> flatMap(String expression, Function<? super XPathHelper, ? extends Stream<? extends T>> mapper) {
			return nodes(expression).stream()
				.flatMap(mapper)
				.collect(Collectors.toList());
		}
		
		public List<XPathHelper> nodes(String expression) {
			String newExpression;		
			if(expression.startsWith("/")) {
				newExpression = expression;
			} else {
				newExpression = this.expression + "/" + expression;
			}
			
			try {
				NodeList nl = (NodeList)xpath.evaluate(expression, item, XPathConstants.NODESET);
				
				return new AbstractList<XPathHelper>() {
	
					@Override
					public XPathHelper get(int index) {					
						return new XPathHelper(xpath, nl.item(index), newExpression, namespaces);
					}
	
					@Override
					public int size() {
						return nl.getLength();
					}
					
				};
			} catch(XPathExpressionException e) {
				throw new IllegalArgumentException("invalid xpath expression: " + expression, e);
			}
		}
		
		public Optional<XPathHelper> node(String expression) {
			List<XPathHelper> nodes = nodes(expression);
			if(nodes.isEmpty()) {
				return Optional.empty();
			}
			
			if(nodes.size() > 1) {
				throw new IllegalArgumentException("multiple results for: " + expression);
			}
			
			return Optional.of(nodes.get(0));
		}
		
		public Optional<Integer> integer() {
			return string().map(Integer::parseInt);
		}
		
		public Integer integerOrNull() {
			return integer().orElse(null);
		}
		
		public Optional<String> string() {
			String retval = item.getTextContent();
			if(retval.trim().isEmpty()) {
				return Optional.empty();
			}
			
			return Optional.of(retval);
		}
		
		public String stringOrNull() {
			return string().orElse(null);
		}
		
		public Optional<Integer> integer(String expression) {			
			return node(expression).flatMap(node -> node.integer());
		}
		
		public Integer integerOrNull(String expression) {
			return integer(expression).orElse(null);
		}
		
		public Optional<String> string(String expression) {			
			return node(expression).flatMap(node -> node.string());
		}
		
		public String stringOrNull(String expression) {
			return string(expression).orElse(null);
		}
		
		public void setTextContent(String textContent) {
			item.setTextContent(textContent);
		}

		public boolean isTextOnly() {
			NodeList children = item.getChildNodes();
			for(int i = 0; i < children.getLength(); i++) {
				if(children.item(i).getNodeType() != Node.TEXT_NODE) {
					return false;
				}
			}
			
			return true;
		}

		public void remove() {
			item.getParentNode().removeChild(item);
		}
		
		private QName toQName(String name) {
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
		
		private String getQualifiedName(QName qName) {
			String prefix = item.lookupPrefix(qName.getNamespaceURI());
			
			if(prefix == null) {
				return qName.getLocalPart();
			} else {
				return prefix + ":" + qName.getLocalPart();
			}
		}
		
		private Element createElement(String name, String content, Map<String, String> attributes) {
			if(name.contains("/")) {
				int separatorIndex = name.indexOf("/");
				
				Element newElement = createElement(name.substring(0, separatorIndex), null, null);
				newElement.appendChild(createElement(name.substring(separatorIndex + 1), content, attributes));
				
				return newElement;
			} else {
				QName qName = toQName(name);
				
				Document document = item.getOwnerDocument();
				
				Element newElement = document.createElementNS(qName.getNamespaceURI(), getQualifiedName(qName));
				
				if(content != null) {
					newElement.appendChild(document.createTextNode(content));
				}		
				
				if(attributes != null) {
					for(Map.Entry<String, String> attribute : attributes.entrySet()) {
						QName attributeName = toQName(attribute.getKey());
						
						newElement.setAttributeNS(attributeName.getNamespaceURI(), getQualifiedName(attributeName), attribute.getValue());
					}
				}
				
				return newElement;
			}
		}

		public String createElement(String name, String content, Map<String, String> attributes, String[] followingSiblings) {
			Element newElement = createElement(name, content, attributes);
			QName newElementName = new QName(newElement.getNamespaceURI(), newElement.getLocalName());
			
			int sameElementCount = 1;
			
			Set<QName> followingSiblingsSet = new HashSet<QName>();
			if(followingSiblings != null) {
				for(String followingSibling : followingSiblings) {
					followingSiblingsSet.add(toQName(followingSibling));
				}
			}
			
			NodeList children = item.getChildNodes();
			
			for(int i = 0; i < children.getLength(); i++) {
				Node childNode = children.item(i);
				if(childNode.getNodeType() == Node.ELEMENT_NODE) {
					QName childName = new QName(childNode.getNamespaceURI(), childNode.getLocalName());
					
					if(followingSiblingsSet.contains(childName)) {
						item.insertBefore(newElement, childNode);
						
						return getResultXPath(name, sameElementCount);
					}
					
					if(newElementName.equals(childName)) {
						sameElementCount++;
					}
				}
			}			
			
			item.appendChild(newElement);
			
			return getResultXPath(name, sameElementCount);
		}
		
		private String getResultXPath(String name, int sameElementCount) {
			if(name.contains("/")) {
				int separatorIndex = name.indexOf("/");
				
				return expression + "/" + name.substring(0, separatorIndex) 
						+ "[" + sameElementCount + "]/"
						+ name.substring(separatorIndex + 1);
			} else {
				return expression + "/" + name + "[" + sameElementCount + "]";
			}
		}
	
	}
	
	public static XPathHelper xpath(Document document) {
		return xpath(document, Optional.empty());
	}
	
	public static XPathHelper xpath(Document document, Map<String, String> namespaces) {
		return xpath(document, Optional.of(namespaces));
	}
	
	public static XPathHelper xpath(Document document, Optional<Map<String, String>> optionalNamespaces) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		BiMap<String, String> namespaces;
		if(optionalNamespaces.isPresent()) {
			namespaces = HashBiMap.create(optionalNamespaces.get());
			
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
				public Iterator<String> getPrefixes(String namespaceURI) {
					return Collections.singletonList(namespaceURI).iterator();
				}
				
			});
		} else {
			namespaces = HashBiMap.create();
		}
		
		return new XPathHelper(xpath, document, "/" ,namespaces);
	}
	
	public static boolean equalsIgnoreWhitespace(Document a, Document b) throws XMLStreamException {
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLEventFactory xef = XMLEventFactory.newInstance();
		
		return equals(xif, xef, a, b,
			Optional.of(event ->			
				event.isCharacters() 
					? xef.createCharacters(event.asCharacters().getData().trim())
					: event),
				
			Optional.of(event -> 
				!event.isCharacters() 
					|| !event.asCharacters().getData().isEmpty()));
	}
	
	public static boolean equals(Document a, Document b) throws XMLStreamException {
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLEventFactory xef = XMLEventFactory.newInstance();
		
		return equals(xif, xef, a, b, Optional.empty(), Optional.empty());
	}
	
	private static <T> List<T> mapFilter(List<T> input, Optional<Function<? super T, ? extends T>> mapper, Optional<Predicate<? super T>> filter) {
		return input.stream()
			.map(mapper.orElse(Function.identity()))
			.filter(filter.orElse(item -> true))			
			.collect(Collectors.toList());
	}
	
	private static boolean equals(XMLInputFactory xif, XMLEventFactory xef, Document a, Document b, 
			Optional<Function<? super XMLEvent, ? extends XMLEvent>> mapper, 
			Optional<Predicate<? super XMLEvent>> filter) throws XMLStreamException {
		
		List<XMLEvent> listA = toEventList(xif, xef, a);
		List<XMLEvent> listB = toEventList(xif, xef, b);
		
		return mapFilter(listA, mapper, filter)
			.equals(mapFilter(listB, mapper, filter));
	}
	
	public static List<XMLEvent> toEventList(Document document) throws XMLStreamException {
		XMLInputFactory xif = XMLInputFactory.newInstance();
		XMLEventFactory xef = XMLEventFactory.newInstance();
		
		return toEventList(xif, xef, document);
	}

	private static List<XMLEvent> toEventList(XMLInputFactory xif, XMLEventFactory xef, Document document) throws XMLStreamException {
		StringBuilder sb = new StringBuilder();
		List<XMLEvent> events = new ArrayList<>();		
		XMLEventReader xer = xif.createXMLEventReader(new DOMSource(document));		
		
		while(xer.hasNext()){
			XMLEvent event = xer.nextEvent();
			if(event.isCharacters()) { // merge subsequent characters events
				sb.append(event.asCharacters().getData());
			} else {
				if(sb.length() > 0) {
					events.add(xef.createCharacters(sb.toString()));
					sb.setLength(0);
				}
				
				events.add(event);				
			}
		}
		
		return events;
	}
}