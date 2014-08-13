package nl.idgis.publisher.harvester.metadata;

import nl.idgis.publisher.harvester.metadata.messages.ParseMetadataDocument;
import nl.idgis.publisher.xml.XMLDocumentFactory;
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
					ActorRef xmlDocument = (ActorRef)msg;					
					
					log.debug("metadata document parsed");
					
					sender.tell(getContext().actorOf(MetadataDocument.props(xmlDocument)), getSelf());
				}
				
			}, getContext().dispatcher());
	}

}
