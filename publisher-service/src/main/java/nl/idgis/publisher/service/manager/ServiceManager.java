package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QPublishedServiceStyle.publishedServiceStyle;
import static nl.idgis.publisher.database.QEnvironment.environment;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;

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
import nl.idgis.publisher.stream.IteratorCursor;
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
			handleGetPublishedServiceIndex((GetPublishedServiceIndex)msg);
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
			IteratorCursor.props(msg.getStyles().iterator()), 
			nameGenerator.getName(IteratorCursor.class));
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
	
	private <T, U> Map<T, List<U>> removeNull(Map<T, List<U>> map) {		
		return map.entrySet().stream()
			.collect(Collectors.toMap(
				entry -> entry.getKey(),
				entry -> entry.getValue().stream()
					.filter(item -> item != null)
					.collect(Collectors.toList())));
	}
	
	private void handleGetPublishedServiceIndex(GetPublishedServiceIndex msg) {
		ActorRef sender = getSender();
		db.transactional(tx ->
			tx.query().from(environment)
				.leftJoin(publishedServiceEnvironment).on(publishedServiceEnvironment.environmentId.eq(environment.id))
				.leftJoin(service).on(service.id.eq(publishedServiceEnvironment.serviceId))
				.leftJoin(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.list(environment.identification, genericLayer.name).<List<TypedList<Tuple>>>thenCompose(services ->
					tx.query().from(environment)
						.leftJoin(publishedServiceEnvironment).on(publishedServiceEnvironment.environmentId.eq(environment.id))
						.leftJoin(publishedServiceStyle).on(publishedServiceStyle.serviceId.eq(publishedServiceEnvironment.serviceId))
						.list(environment.identification, publishedServiceStyle.name).<List<TypedList<Tuple>>>thenApply(styles ->
							Arrays.asList(services, styles)))).thenAccept(result -> {
								Map<String, List<String>> services = 
									removeNull(result.get(0).list().stream()
										.collect(Collectors.groupingBy(
											service -> service.get(environment.identification),
											Collectors.mapping(
												service -> service.get(genericLayer.name),
												Collectors.toList()))));
								
								Map<String, List<String>> styles = 
									removeNull(result.get(1).list().stream()										 
										.collect(Collectors.groupingBy(
											style -> style.get(environment.identification),
											Collectors.mapping(
												style -> style.get(publishedServiceStyle.name),
												Collectors.toList()))));
								
								Iterator<PublishedServiceIndex> itr =
									services.entrySet().stream()
										.map(entry -> new PublishedServiceIndex(
											entry.getKey(), 
											entry.getValue(), 
											styles.get(entry.getKey())))
										.collect(Collectors.toList()).iterator();
								
								ActorRef cursor = getContext().actorOf(
										IteratorCursor.props(itr),
										nameGenerator.getName(IteratorCursor.class));
								cursor.tell(new NextItem(), sender);
							});
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
