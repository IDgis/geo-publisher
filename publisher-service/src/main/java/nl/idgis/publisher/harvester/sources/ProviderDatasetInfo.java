package nl.idgis.publisher.harvester.sources;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import scala.concurrent.Future;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.harvest.DatabaseLog;
import nl.idgis.publisher.domain.job.harvest.HarvestLogType;
import nl.idgis.publisher.domain.job.harvest.HarvestLog;
import nl.idgis.publisher.domain.job.harvest.MetadataField;
import nl.idgis.publisher.domain.job.harvest.MetadataLog;
import nl.idgis.publisher.domain.job.harvest.MetadataLogType;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.web.EntityType;

import nl.idgis.publisher.harvester.sources.messages.Finished;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.Ask;
import nl.idgis.publisher.xml.exceptions.NotFound;
import nl.idgis.publisher.xml.exceptions.NotParseable;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class ProviderDatasetInfo extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef harvesterSession, providerDatabase;
	
	private MetadataDocumentFactory metadataDocumentFactory;
	
	public ProviderDatasetInfo(ActorRef harvesterSession, ActorRef providerDatabase) {
		this.harvesterSession = harvesterSession;		
		this.providerDatabase = providerDatabase;		
	}
	
	public static Props props(ActorRef harvesterSession, ActorRef providerDatabase) {
		return Props.create(ProviderDatasetInfo.class, harvesterSession, providerDatabase);
	}
	
	@Override
	public void preStart() throws Exception {
		metadataDocumentFactory = new MetadataDocumentFactory();
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof DatasetInfo) {
			handleDatasetInfo((DatasetInfo)msg);
		} else if(msg instanceof End) {	
			handleEnd();
		} else if(msg instanceof Failure) {
			handleFailure(msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleFailure(Object msg) {
		log.error(msg.toString());
		
		finish();
	}

	private void handleEnd() {
		log.debug("dataset retrieval completed");
		
		finish();
	}

	private void handleDatasetInfo(DatasetInfo DatasetInfo) throws Exception {
		log.debug("dataset info item received");
		
		
	}
	
	private void finish() {
		harvesterSession.tell(new Finished(), getSelf());
		getContext().stop(getSelf());
	}
}
