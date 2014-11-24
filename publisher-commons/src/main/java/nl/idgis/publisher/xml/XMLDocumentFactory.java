package nl.idgis.publisher.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.idgis.publisher.xml.messages.NotParseable;
import nl.idgis.publisher.xml.messages.ParseDocument;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class XMLDocumentFactory extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private DocumentBuilder documentBuilder;	
	
	public static Props props() {
		return Props.create(XMLDocumentFactory.class);
	}
	
	@Override
	public void preStart() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		documentBuilder = dbf.newDocumentBuilder();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ParseDocument) {
			handleParseDocument((ParseDocument)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleParseDocument(ParseDocument msg) throws Exception {
		log.debug("parsing xml document");		
		
		Document document;
		try {
			document = documentBuilder.parse(new ByteArrayInputStream(msg.getContent()));
		} catch (SAXException | IOException e) {
			getSender().tell(new NotParseable(e.getMessage()), getSelf());
			
			throw new IllegalArgumentException("Content not parseable", e);
		}
		
		getSender().tell(new XMLDocument(document), getSelf());
	}
}
