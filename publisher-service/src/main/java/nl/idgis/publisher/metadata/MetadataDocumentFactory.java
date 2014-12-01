package nl.idgis.publisher.metadata;

import nl.idgis.publisher.metadata.messages.ParseMetadataDocument;
import nl.idgis.publisher.xml.XMLDocument;
import nl.idgis.publisher.xml.XMLDocumentFactory;
import nl.idgis.publisher.xml.messages.NotParseable;
import nl.idgis.publisher.xml.messages.ParseDocument;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MetadataDocumentFactory extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private XMLDocumentFactory xmlDocumentFactory;
	
	public static Props props() {
		return Props.create(MetadataDocumentFactory.class);
	}
	
	@Override
	public void preStart() throws Exception {
		xmlDocumentFactory = new XMLDocumentFactory();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ParseMetadataDocument) {
			handleParseMetadataDocument((ParseMetadataDocument)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleParseMetadataDocument(ParseDocument msg) throws Exception {
		log.debug("parsing metadata document");
		
		final ActorRef sender = getSender();
		
		try {
			XMLDocument xmlDocument = xmlDocumentFactory.parseDocument(msg.getContent());
			
			log.debug("metadata document parsed");
			sender.tell(new MetadataDocument(xmlDocument), getSelf());
		} catch(NotParseable np) {
			log.debug("xml parsing error: " + np);
			
			sender.tell(np, getSelf());
		}
	}

}
