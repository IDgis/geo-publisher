package nl.idgis.publisher.metadata;

import java.util.Objects;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.metadata.messages.MetadataInfo;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.UniqueNameGenerator;

/**
 * This actor is responsible for initializing the metadata generation. 
 * It is activated by sending a {@link GenerateMetadata} message.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class MetadataGenerator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
		
	private final ActorRef database, metadataSource;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	public MetadataGenerator(ActorRef database, ActorRef metadataSource) {
		this.database = database;
		this.metadataSource = metadataSource;		
	}
	
	/**
	 * Creates a {@link Props} for the {@link MetadataGenerator} actor.
	 * 
	 * @param database a reference to the database actor.
	 * @param metadataSource a reference to the metadata source actor. 
	 * @return the props.
	 */
	public static Props props(ActorRef database, ActorRef metadataSource) {
		return Props.create(
			MetadataGenerator.class, 
			Objects.requireNonNull(database, "database must not be null"), 
			Objects.requireNonNull(metadataSource, "metadataSource must not be null"));
	}
	
	@Override
	public void preStart() throws Exception {		
		f = new FutureUtils(getContext());
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GenerateMetadata) {
			handleGenerateMetadata((GenerateMetadata)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private void handleGenerateMetadata(GenerateMetadata msg) throws Exception {		
		String environmentId = msg.getEnvironmentId();
		
		log.info("generating metadata for environment: {}", environmentId);
		
		ActorRef processor = getContext().actorOf(
			MetadataInfoProcessor.props(
				getSender(), 
				metadataSource,
				msg.getTarget(),
				msg.getServiceLinkagePrefix(),
				msg.getDatasetMetadataPrefix()),
			
			nameGenerator.getName(MetadataInfoProcessor.class));

		MetadataInfo.fetch(db.query(), environmentId)
			.whenComplete((metadataInfo, throwable) ->				 
				processor.tell(
					throwable == null 
						? metadataInfo 
						: new Failure(throwable),
					getSelf()));
	}
}