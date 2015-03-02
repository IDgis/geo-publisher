package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QStyle.style;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.service.manager.messages.GetGroupLayer;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.GetServiceIndex;
import nl.idgis.publisher.service.manager.messages.GetServicesWithLayer;
import nl.idgis.publisher.service.manager.messages.GetStyles;
import nl.idgis.publisher.service.manager.messages.ServiceIndex;
import nl.idgis.publisher.service.manager.messages.Style;
import nl.idgis.publisher.stream.ListCursor;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedIterable;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class ServiceManager extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final ActorRef database;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	private static class CreateStyleCursor {
		
		private final List<Style> styles;
		
		CreateStyleCursor(List<Style> styles) {
			this.styles = styles;
		}
		
		
		List<Style> getStyles() {
			return styles;
		}
	}
	
	public ServiceManager(ActorRef database) {
		this.database = database;
	}
	
	public static Props props(ActorRef database) {
		return Props.create(ServiceManager.class, database);
	}
	
	@Override
	public void preStart() throws Exception {
		f = new FutureUtils(getContext().dispatcher());
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetService) {
			toSender(handleGetService((GetService)msg));
		} else if(msg instanceof GetGroupLayer) {
			toSender(handleGetGroupLayer((GetGroupLayer)msg));
		} else if(msg instanceof GetServicesWithLayer) {
			toSender(handleGetServicesWithLayer((GetServicesWithLayer)msg));
		} else if(msg instanceof GetServiceIndex) {
			toSender(handleGetServiceIndex((GetServiceIndex)msg));
		} else if(msg instanceof GetStyles) {
			handleGetStyles((GetStyles)msg);
		} else if(msg instanceof CreateStyleCursor) {
			handleCreateStyleCursor((CreateStyleCursor)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private void handleCreateStyleCursor(CreateStyleCursor msg) {
		ActorRef cursor = getContext().actorOf(
			ListCursor.props(msg.getStyles().iterator()), 
			nameGenerator.getName(ListCursor.class));
		cursor.tell(new NextItem(), getSender());
	}

	private void handleGetStyles(GetStyles msg) {
		ActorRef self = getSelf(), sender = getSender();
		new GetStylesQuery(f, db, msg.getServiceId()).result().whenComplete((result, t) -> {
			if(t == null) {
				self.tell(new CreateStyleCursor(result), sender);
			} else {
				sender.tell(new Failure(t), self);
			}
		});
	}

	private CompletableFuture<ServiceIndex> handleGetServiceIndex(GetServiceIndex msg) {
		return db.transactional(tx ->			 
			tx.query()
				.from(service)
				.list(service.name).thenCompose(serviceNames -> 
					tx.query()
						.from(style)
						.list(style.name).thenApply(styleNames -> 
							new ServiceIndex(
								serviceNames.list(), 
								styleNames.list()))));
	}

	private void toSender(CompletableFuture<? extends Object> future) {
		ActorRef sender = getSender(), self = getSelf();
		
		future.whenComplete((resp, t) -> {
			if(t != null) {
				sender.tell(new Failure(t), self);
			} else {
				sender.tell(resp, self);
			}
		});
	}
	
	private CompletableFuture<TypedIterable<String>> handleGetServicesWithLayer(GetServicesWithLayer msg) {
		return db.transactional(tx -> new GetServicesWithLayerQuery(f, tx, msg.getLayerId()).result());
	}
	
	private CompletableFuture<Object> handleGetGroupLayer(GetGroupLayer msg) {
		return db.transactional(tx -> new GetGroupLayerQuery(f, tx, msg.getGroupLayerId()).result()); 
	}

	private CompletableFuture<Object> handleGetService(GetService msg) {
		return db.transactional(tx -> new GetServiceQuery(f, tx, msg.getServiceId()).result());
	}	
}
