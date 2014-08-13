package nl.idgis.publisher.xml;

import java.util.Arrays;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.xml.messages.Close;
import nl.idgis.publisher.xml.messages.GetString;
import nl.idgis.publisher.xml.messages.NotFound;
import nl.idgis.publisher.xml.messages.Query;

import org.w3c.dom.Document;

import com.google.common.collect.BiMap;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class XMLDocument extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Document document;
	
	private XPathFactory xf;
	
	public XMLDocument(Document document) {
		this.document = document;
	}
	
	public static Props props(Document document) {
		return Props.create(XMLDocument.class, document);
	}
	
	@Override
	public void preStart() throws Exception {
		 xf = XPathFactory.newInstance();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Query) {
			handleQuery((Query)msg);
		} else if(msg instanceof Close) {
			handleClose((Close)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleClose(Close msg) {
		log.debug("closing document");
		
		getSender().tell(new Ack(), getSelf());
		getContext().stop(getSelf());
	}

	private void handleQuery(final Query msg) throws Exception {
		
		XPath xpath = xf.newXPath();
		xpath.setNamespaceContext(new NamespaceContext() {
			
			BiMap<String, String> namespaces = msg.getNamespaces();

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
		
		String expression = msg.getExpression();
		log.debug("evaluating expression: " + expression);
		
		if(msg instanceof GetString) {
			try {				
				getSender().tell(xpath.evaluate(expression, document), getSelf());
			} catch(Exception e) {
				getSender().tell(new NotFound(), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
}
