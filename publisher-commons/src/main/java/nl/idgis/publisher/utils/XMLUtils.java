package nl.idgis.publisher.utils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLUtils {
	
	public static class XPathHelper {
	
		private final XPath xpath;
		
		private final Node item;
		
		private XPathHelper(XPath xpath, Node item) {
			this.xpath = xpath;
			this.item = item;
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
			try {
				NodeList nl = (NodeList)xpath.evaluate(expression, item, XPathConstants.NODESET);
				
				return new AbstractList<XPathHelper>() {
	
					@Override
					public XPathHelper get(int index) {					
						return new XPathHelper(xpath, nl.item(index));
					}
	
					@Override
					public int size() {
						return nl.getLength();
					}
					
				};
			} catch(XPathExpressionException e) {
				throw new IllegalArgumentException("invalid xpath expression: " + expression);
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
	
	}
	
	public static XPathHelper xpath(Document document) {
		return new XPathHelper(XPathFactory.newInstance().newXPath(), document);
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
	
	private static boolean equals(List<XMLEvent> a, List<XMLEvent> b) {
		Iterator<XMLEvent> aItr = a.iterator();
		Iterator<XMLEvent> bItr = b.iterator();
		
		while(aItr.hasNext() && bItr.hasNext()) {
			XMLEvent aItem = aItr.next();			
			XMLEvent bItem = bItr.next();
			
			int aEventType = aItem.getEventType();
			int bEventType = bItem.getEventType();
			
			if(aEventType != bEventType) {
				return false;
			}
			
			switch(aEventType) {
				case XMLStreamConstants.START_ELEMENT:
					if(!aItem.asStartElement().getName().equals(bItem.asStartElement().getName())) {
						return false;
					}
					
					break;
				case XMLStreamConstants.END_ELEMENT:
					if(!aItem.asEndElement().getName().equals(bItem.asEndElement().getName())) {
						return false;
					}
					
					break;
				case XMLStreamConstants.CHARACTERS:
					if(!aItem.asCharacters().getData().equals(bItem.asCharacters().getData())) {
						return false;
					}
					
					break;
			}
		}
		
		return aItr.hasNext() == bItr.hasNext();
	}
	
	private static boolean equals(XMLInputFactory xif, XMLEventFactory xef, Document a, Document b, 
			Optional<Function<? super XMLEvent, ? extends XMLEvent>> mapper, 
			Optional<Predicate<? super XMLEvent>> filter) throws XMLStreamException {
		
		List<XMLEvent> listA = toEventList(xif, xef, a);
		List<XMLEvent> listB = toEventList(xif, xef, b);
		
		return equals(
			mapFilter(listA, mapper, filter),
			mapFilter(listB, mapper, filter));
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