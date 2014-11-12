package nl.idgis.publisher.metadata;

import nl.idgis.publisher.metadata.messages.ParseMetadataDocument;
import nl.idgis.publisher.xml.XMLDocument;
import nl.idgis.publisher.xml.XMLDocumentFactory;
import nl.idgis.publisher.xml.messages.NotParseable;
import nl.idgis.publisher.xml.messages.ParseDocument;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class MetadataDocumentFactory extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef xmlDocumentFactory;
	
	public static Props props() {
		return Props.create(MetadataDocumentFactory.class);
	}
	
	@Override
	public void preStart() throws Exception {
		xmlDocumentFactory = getContext().actorOf(XMLDocumentFactory.props(), "xmlDocumentFactory");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ParseMetadataDocument) {
			handleParseMetadataDocument((ParseMetadataDocument)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleParseMetadataDocument(ParseDocument msg) {
		log.debug("parsing metadata document");
		
		final ActorRef sender = getSender();
		
		Patterns.ask(xmlDocumentFactory, msg, 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					if(msg instanceof NotParseable) {
						log.debug("xml parsing error: " + msg); 
						
						sender.tell(msg, getSelf());
					} else {					
						XMLDocument xmlDocument = (XMLDocument)msg;					
						
						log.debug("metadata document parsed");
						
						sender.tell(new MetadataDocument(xmlDocument), getSelf());
					}
				}
				
			}, getContext().dispatcher());
	}

}
