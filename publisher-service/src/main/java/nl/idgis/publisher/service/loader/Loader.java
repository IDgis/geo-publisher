package nl.idgis.publisher.service.loader;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import nl.idgis.publisher.harvester.messages.RequestDataset;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.service.loader.messages.ImportDataset;
import nl.idgis.publisher.stream.messages.End;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class Loader extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef database, harvester;

	public Loader(ActorRef database, ActorRef harvester) {
		this.database = database;
		this.harvester = harvester;
	}
	
	public static Props props(ActorRef database, ActorRef harvester) {
		return Props.create(Loader.class, database, harvester);
	}
	
	@Override
	public void preStart() throws Exception {
		FiniteDuration interval = Duration.create(10, TimeUnit.SECONDS);
		getContext().system().scheduler().scheduleOnce(interval, getSelf(), new ImportDataset("my-provider-name", "400483ec-feee-4bab-92f2-073ffe6c9be7.xml"), getContext().dispatcher(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ImportDataset) {
			ImportDataset importDataset = (ImportDataset)msg;

			harvester.tell(new RequestDataset(importDataset.getDataSourceId(), importDataset.getSourceDatasetId(), getSelf()), getSelf());
			getContext().become(importing(), true);
		} else {
			unhandled(msg);
		}
	}

	private Procedure<Object> importing() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {			
				if(msg instanceof Record) {
					log.debug("record received: " + msg);
					
				} else if(msg instanceof Failure) {
					log.error("import failed: " + ((Failure) msg).getCause());
					
					getContext().unbecome();
				} else if(msg instanceof End) {
					log.info("import completed");
					
					getContext().unbecome();
				}
			}
		};
	}
}
