package nl.idgis.publisher.metadata;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QServiceJob.serviceJob;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.PublisherDatabase;
import nl.idgis.publisher.database.QJobState;
import nl.idgis.publisher.database.QServiceJob;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.sources.ProviderDataSource;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;
import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;
import nl.idgis.publisher.xml.exceptions.NotFound;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.typesafe.config.Config;

public class MetadataGenerator extends UntypedActor {
	
	private static final String ENDPOINT_CODE_LIST_VALUE = "WebServices";

	private static final String ENDPOINT_CODE_LIST = "http://www.isotc211.org/2005/iso19119/resources/Codelist/gmxCodelists.xml#DCPList";

	private static final String ENDPOINT_OPERATION_NAME = "GetCapabilitities";

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorSystem system;
	
	private final ActorRef database, harvester;
	
	private AsyncDatabaseHelper db;
	
	private final MetadataStore serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget;
	
	private final Config constants;
	
	private FutureUtils f;
	
	public MetadataGenerator(ActorRef database, ActorRef harvester, MetadataStore serviceMetadataSource, MetadataStore datasetMetadataTarget, MetadataStore serviceMetadataTarget, Config constants) {
		this.database = database;		
		this.harvester = harvester;
		this.serviceMetadataSource = serviceMetadataSource;
		this.datasetMetadataTarget = datasetMetadataTarget;
		this.serviceMetadataTarget = serviceMetadataTarget;
		this.constants = constants;
		
//		system = ActorSystem.create("test", akkaConfig);
//		f = new FutureUtils(system);
		
	}
	
	public static Props props(ActorRef database, ActorRef harvester, MetadataStore serviceMetadataSource, MetadataStore datasetMetadataTarget, MetadataStore serviceMetadataTarget, Config constants) {
		return Props.create(MetadataGenerator.class, database, harvester, serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget, constants);
	}
	
	@Override
	public void preStart() throws Exception {		
		f = new FutureUtils(getContext());
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GenerateMetadata) {
			generateMetadata();
		} else {
			unhandled(msg);
		}
	}

	private void generateMetadata() throws InterruptedException, ExecutionException, NotFound {		
		
		final ActorRef sender = getSender();		
		// TODO: implement metadata generator
		
//		log.info("generating metadata");
		
//		MetadataDocument mdDoc = f.ask(harvester, new GetDatasetMetadata("e8c64317-4e4c-4afb-bdf1-9e0a38e2a924"), MetadataDocument.class).get();
//		
//		log.info("MetadataDocument " + mdDoc);
//		log.info("Dataset title " + mdDoc.getDatasetTitle());
//		
//		sender.tell(new Ack(), getSelf());
	}
	
	
}
