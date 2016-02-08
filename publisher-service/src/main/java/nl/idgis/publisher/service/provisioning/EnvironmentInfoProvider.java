package nl.idgis.publisher.service.provisioning;

import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QService.service;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.service.provisioning.messages.GetEnvironments;
import nl.idgis.publisher.utils.FutureUtils;

public class EnvironmentInfoProvider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	public EnvironmentInfoProvider(ActorRef database) {
		this.database = database;
	}
	
	public static Props props(ActorRef database) {
		return Props.create(EnvironmentInfoProvider.class, database);
	}
	
	@Override
	public void preStart() throws Exception {
		f = new FutureUtils(getContext());
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public void onReceive(Object msg) throws Exception { 
		if(msg instanceof GetEnvironments) {			
			GetEnvironments getEnvironmentIds = (GetEnvironments)msg;
			
			String serviceId = getEnvironmentIds.getServiceId();
			log.debug("fetching environments for service: {}", serviceId);
			
			ActorRef sender = getSender();
			db.transactional(getEnvironmentIds, tx ->
				tx.query().from(service)
					.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
					.join(publishedService).on(publishedService.serviceId.eq(service.id))
					.join(environment).on(environment.id.eq(publishedService.environmentId))
					.where(genericLayer.identification.eq(serviceId))
					.list(environment.identification)).thenAccept(environmentIds -> 
						sender.tell(environmentIds, getSelf()));
		} else {
			unhandled(msg);
		}
	}

}
