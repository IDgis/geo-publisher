package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QStyle.style;
import static java.util.Collections.emptyMap;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.domain.web.tree.Service;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.service.manager.messages.GetDatasetLayerRef;
import nl.idgis.publisher.service.manager.messages.GetGroupLayer;
import nl.idgis.publisher.service.manager.messages.GetPublishedService;
import nl.idgis.publisher.service.manager.messages.GetPublishedServiceIndex;
import nl.idgis.publisher.service.manager.messages.GetPublishedStyles;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.GetServiceIndex;
import nl.idgis.publisher.service.manager.messages.GetServicesWithDataset;
import nl.idgis.publisher.service.manager.messages.GetServicesWithLayer;
import nl.idgis.publisher.service.manager.messages.GetServicesWithStyle;
import nl.idgis.publisher.service.manager.messages.GetStyles;
import nl.idgis.publisher.service.manager.messages.PublishService;
import nl.idgis.publisher.service.manager.messages.PublishedServiceIndex;
import nl.idgis.publisher.service.manager.messages.ServiceIndex;
import nl.idgis.publisher.service.manager.messages.Style;
import nl.idgis.publisher.stream.ListCursor;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;
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
		f = new FutureUtils(getContext());
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
		} else if (msg instanceof GetServicesWithDataset) {
			toSender (handleGetServicesWithDataset ((GetServicesWithDataset) msg));
		} else if(msg instanceof GetServiceIndex) {
			toSender(handleGetServiceIndex((GetServiceIndex)msg));
		} else if(msg instanceof GetServicesWithStyle) {
			toSender(handleGetServicesWithStyle((GetServicesWithStyle)msg));
		} else if(msg instanceof GetStyles) {
			handleGetStyles((GetStyles)msg);
		} else if(msg instanceof CreateStyleCursor) {
			handleCreateStyleCursor((CreateStyleCursor)msg);
		} else if(msg instanceof GetDatasetLayerRef) {
			toSender(handleGetDatasetLayerRef((GetDatasetLayerRef)msg));
		} else if(msg instanceof PublishService) {
			toSender(handlePublishService((PublishService)msg));
		} else if(msg instanceof GetPublishedService) {
			toSender(handleGetPublishedService((GetPublishedService)msg));
		} else if(msg instanceof GetPublishedStyles) {
			handleGetPublishedStyles((GetPublishedStyles)msg);
		} else if(msg instanceof GetPublishedServiceIndex) {
			toSender(handleGetPublishedServiceIndex((GetPublishedServiceIndex)msg));
		} else {
			unhandled(msg);
		}
	}
	
	private void handleGetPublishedStyles(GetPublishedStyles msg) {
		ActorRef self = getSelf(), sender = getSender();
		db.transactional(msg, tx -> 
			new GetPublishedStylesQuery(log, f, tx, msg.getServiceId()).result()).whenComplete((result, t) -> {
				if(t == null) {
					self.tell(new CreateStyleCursor(result), sender);
				} else {
					sender.tell(new Failure(t), self);
				}
			});
	}

	private CompletableFuture<Object> handleGetPublishedService(GetPublishedService msg) {
		return db.transactional(msg, tx -> new GetPublishedServiceQuery(log, f, tx, msg.getServiceId()).result());
	}

	private CompletableFuture<Ack> handlePublishService(PublishService msg) {
		return 
			db.transactional(msg, tx ->
				f.ask(
					getSelf(), 
					new GetService(
						Optional.of(tx.getTransactionRef()), 
						msg.getServiceId()), 
						Service.class).thenCompose(service ->
							new PublishServiceQuery(log, f, tx, service, msg.getEnvironmentIds())
								.result()));
	}

	private CompletableFuture<Object> handleGetDatasetLayerRef(GetDatasetLayerRef msg) {
		return db.transactional(tx -> new GetDatasetLayerRefQuery(log, tx, msg.getLayerId()).result());
	}

	private CompletableFuture<TypedList<String>> handleGetServicesWithStyle(GetServicesWithStyle msg) {
		return db.transactional(tx -> new GetServicesWithStyleQuery(log, f, tx, msg.getStyleId()).result());
	}

	private void handleCreateStyleCursor(CreateStyleCursor msg) {
		ActorRef cursor = getContext().actorOf(
			ListCursor.props(msg.getStyles().iterator()), 
			nameGenerator.getName(ListCursor.class));
		cursor.tell(new NextItem(), getSender());
	}

	private void handleGetStyles(GetStyles msg) {
		ActorRef self = getSelf(), sender = getSender();
		db.transactional(msg, tx -> 
			new GetStylesQuery(log, f, tx, msg.getServiceId()).result()).whenComplete((result, t) -> {
				if(t == null) {
					self.tell(new CreateStyleCursor(result), sender);
				} else {
					sender.tell(new Failure(t), self);
				}
			});
	}
	
	private CompletableFuture<PublishedServiceIndex> handleGetPublishedServiceIndex(GetPublishedServiceIndex msg) {
		return f.successful(new PublishedServiceIndex(emptyMap()));					
	}

	private CompletableFuture<ServiceIndex> handleGetServiceIndex(GetServiceIndex msg) {
		return db.transactional(tx ->			 
			tx.query()
				.from(service)
				.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.list(genericLayer.name).thenCompose(serviceNames -> 
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
				if(t instanceof CompletionException) {
					sender.tell(new Failure(t.getCause()), self);
				} else {
					sender.tell(new Failure(t), self);
				}
			} else {
				sender.tell(resp, self);
			}
		});
	}
	
	private CompletableFuture<TypedList<String>> handleGetServicesWithLayer(GetServicesWithLayer msg) {
		return db.transactional(tx -> new GetServicesWithLayerQuery(log, f, tx, msg.getLayerId()).result());
	}
	
	private CompletableFuture<TypedList<String>> handleGetServicesWithDataset (final GetServicesWithDataset msg) {
		return db.transactional (tx -> new GetServicesWithDatasetQuery (log, f, tx, msg.getDatasetId ()).result ());
	}
	
	private CompletableFuture<Object> handleGetGroupLayer(GetGroupLayer msg) {
		return db.transactional(msg, tx -> new GetGroupLayerQuery(log, f, tx, msg.getGroupLayerId()).result());		
	}

	private CompletableFuture<Object> handleGetService(GetService msg) {
		return db.transactional(msg, tx -> new GetServiceQuery(log, f, tx, msg.getServiceId()).result());
	}
}
