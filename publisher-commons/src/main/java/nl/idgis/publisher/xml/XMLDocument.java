package nl.idgis.publisher.xml;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.xml.messages.Close;
import nl.idgis.publisher.xml.messages.GetContent;
import nl.idgis.publisher.xml.messages.GetString;
import nl.idgis.publisher.xml.messages.MultipleNodes;
import nl.idgis.publisher.xml.messages.NotFound;
import nl.idgis.publisher.xml.messages.NotTextOnly;
import nl.idgis.publisher.xml.messages.Query;
import nl.idgis.publisher.xml.messages.UpdateString;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.BiMap;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class XMLDocument extends UntypedActor {
	
	public static Props props(Document document) {
		return Props.create(XMLDocument.class, document);
	}
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Document document;
	
	private XPathFactory xf;
	
	public XMLDocument(Document document) {
		this.document = document;
	}
	
	private void handleClose(Close msg) {
		log.debug("closing document");
		
		getSender().tell(new Ack(), getSelf());
		getContext().stop(getSelf());
	}

	private void handleGetString(final GetString query, XPath xpath,
			String expression) {
		try {
			String s = xpath.evaluate(expression, document);
			if(s.isEmpty()) {
				sendNotFound(query);
			} else {
				getSender().tell(s, getSelf());
			}
		} catch(Exception e) {
			sendNotFound(query);
		}
	}

	private void handleQuery(final Query<?> query) throws Exception {
		
		XPath xpath = xf.newXPath();
		xpath.setNamespaceContext(new NamespaceContext() {
			
			BiMap<String, String> namespaces = query.getNamespaces();

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
		
		String expression = query.getExpression();
		log.debug("evaluating expression: " + expression);
		
		if(query instanceof GetString) {
			handleGetString((GetString)query, xpath, expression);
		} else if(query instanceof UpdateString) {
			handleUpdateQuery((UpdateString)query, xpath, expression);
		} else {
			unhandled(query);
		}
	}

	private void handleUpdateQuery(final UpdateString query, XPath xpath, String expression) {
		
		NodeList nodeList;
		try {
			nodeList = (NodeList)xpath.evaluate(expression, document, XPathConstants.NODESET);
		} catch(Exception e) {
			sendNotFound(query);
			return;
		}
		
		switch(nodeList.getLength()) {
			case 0:
				sendNotFound(query);
				break;
			case 1:
				Node n = nodeList.item(0);
				
				if(isTextOnly(n)) {
					n.setTextContent(((UpdateString) query).getNewValue());
					sendAck();
				} else {
					sendNotTextOnly(query);
				}
				
				break;
			default:
				sendMultipleNodes(query);
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

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Query) {
			handleQuery((Query<?>)msg);
		} else if(msg instanceof GetContent) {
			handleGetContent((GetContent)msg);
		} else if(msg instanceof Close) {
			handleClose((Close)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleGetContent(GetContent msg) throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		t.transform(new DOMSource(document), new StreamResult(boas));
		boas.close();
		
		getSender().tell(boas.toByteArray(), getSelf());
	}

	@Override
	public void preStart() throws Exception {
		 xf = XPathFactory.newInstance();
	}

	private void sendAck() {
		getSender().tell(new Ack(), getSelf());
	}

	private void sendMultipleNodes(Query<?> query) {
		getSender().tell(new MultipleNodes(query), getSelf());
	}

	private void sendNotFound(final Query<?> query) {
		getSender().tell(new NotFound(query), getSelf());
	}

	private void sendNotTextOnly(Query<?> query) {
		getSender().tell(new NotTextOnly(query), getSelf());
	}
}
