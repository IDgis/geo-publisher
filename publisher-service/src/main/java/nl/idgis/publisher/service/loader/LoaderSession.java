package nl.idgis.publisher.service.loader;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.ImportJob;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.domain.log.GenericEvent;
import nl.idgis.publisher.domain.log.ImportLogLine;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class LoaderSession extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ImportJob importJob;
	private final ActorRef geometryDatabase, database;
	
	private long count = 0;
	
	public LoaderSession(ImportJob importJob, ActorRef geometryDatabase, ActorRef database) {
		this.importJob = importJob;
		this.geometryDatabase = geometryDatabase;
		this.database = database;
	}
	
	public static Props props(ImportJob importJob, ActorRef geometryDatabase, ActorRef database) {
		return Props.create(LoaderSession.class, importJob, geometryDatabase, database);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Record) {
			log.debug("record received: " + msg);
			
			count++;			
			getSender().tell(new NextItem(), getSelf());			
		} else if(msg instanceof Failure) {
			log.error("import failed: " + ((Failure) msg).getCause());
			
			final ActorRef self = getSelf();
			Patterns.ask(geometryDatabase, new Rollback(), 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						log.debug("transaction rolled back");
						
						getContext().stop(self);
					}
					
				}, getContext().dispatcher());
		} else if(msg instanceof End) {
			log.info("import completed");
			
			final ActorRef self = getSelf();
			Patterns.ask(geometryDatabase, new Commit(), 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						log.debug("transaction committed");
						
						ImportLogLine logLine = new ImportLogLine(GenericEvent.FINISHED, importJob.getDatasetId());
						Patterns.ask(database, new StoreLog(logLine), 15000)
							.onSuccess(new OnSuccess<Object>() {

								@Override
								public void onSuccess(Object msg) throws Throwable {
									log.debug("import finished: " + count);
									
									getContext().stop(self);
								}					
							}, getContext().dispatcher());
					}
					
				}, getContext().dispatcher());
			
		}
	}
}
