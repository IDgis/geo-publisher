package nl.idgis.publisher.harvester.metadata;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import nl.idgis.publisher.harvester.metadata.messages.GetAlternateTitle;
import nl.idgis.publisher.harvester.metadata.messages.GetTitle;
import nl.idgis.publisher.harvester.metadata.messages.MetadataQuery;
import nl.idgis.publisher.xml.messages.Close;
import nl.idgis.publisher.xml.messages.GetString;
import nl.idgis.publisher.xml.messages.Query;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class MetadataDocument extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef xmlDocument;	
	private final Map<Class<? extends MetadataQuery>, Query> queries;

	public MetadataDocument(ActorRef xmlDocument) {
		this.xmlDocument = xmlDocument;
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("gmd", "http://www.isotc211.org/2005/gmd");
		namespaces.put("gco", "http://www.isotc211.org/2005/gco");
		
		queries = new HashMap<>();
		queries.put(GetTitle.class, new GetString(namespaces, 
				"/gmd:MD_Metadata" +
				"/gmd:identificationInfo" +
				"/gmd:MD_DataIdentification" +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:title" +
				"/gco:CharacterString"));
		
		queries.put(GetAlternateTitle.class, new GetString(namespaces, 
				"/gmd:MD_Metadata" +
				"/gmd:identificationInfo" +
				"/gmd:MD_DataIdentification" +
				"/gmd:citation" +
				"/gmd:CI_Citation" +
				"/gmd:alternateTitle" +
				"/gco:CharacterString"));
	}
	
	public static Props props(ActorRef xmlDocument) {
		return Props.create(MetadataDocument.class, xmlDocument);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Close) {
			handleClose((Close)msg);
		} else if(msg instanceof MetadataQuery) {
			handleMetadataQuery((MetadataQuery)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleMetadataQuery(MetadataQuery msg) {
		Class<? extends MetadataQuery> clazz = msg.getClass();
		if(queries.containsKey(clazz)) {
			log.debug("query dispatched");
			
			xmlDocument.tell(queries.get(clazz), getSender());			
		} else {
			log.error("unknown metadata query: " + msg);
		}
	}

	private void handleClose(Close msg) {
		log.debug("closing metadata document");
		
		final ActorRef sender = getSender();
		Patterns.ask(xmlDocument, msg, 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("xml document closed");
					
					sender.tell(msg, getSelf());
					getContext().stop(getSelf());
				}
				
			}, getContext().dispatcher());
	}

}
