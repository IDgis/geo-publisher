package nl.idgis.publisher.metadata;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.metadata.messages.MetadataInfo;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.UniqueNameGenerator;
import nl.idgis.publisher.xml.exceptions.NotFound;

public class MetadataGenerator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
		
	private final ActorRef database, metadataSource, metadataTarget;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	public MetadataGenerator(ActorRef database, ActorRef metadataSource, ActorRef metadataTarget) {
		this.database = database;
		this.metadataSource = metadataSource;
		this.metadataTarget = metadataTarget;
	}
	
	public static Props props(ActorRef database, ActorRef metadataSource, ActorRef metadataTarget) {
		return Props.create(
			MetadataGenerator.class, 
			Objects.requireNonNull(database, "database must not be null"), 
			Objects.requireNonNull(metadataSource, "metadataSource must not be null"), 
			Objects.requireNonNull(metadataTarget, "metadataTarget must not be null"));
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
		
		log.info("generating metadata");
		
		ActorRef processor = getContext().actorOf(
			MetadataInfoProcessor.props(
				getSender(), 
				metadataSource,
				metadataTarget),
			
			nameGenerator.getName(MetadataInfoProcessor.class));

		MetadataInfo.fetch(db.query()).thenAccept(tuples ->
			processor.tell(
				new MetadataInfo(tuples.list()), 
				getSelf()));
	}
}