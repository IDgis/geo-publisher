package nl.idgis.publisher.service.manager;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.mysema.query.sql.SQLCommonQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;
import com.mysema.query.Tuple;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.QGenericLayer;

import nl.idgis.publisher.protocol.messages.Failure;

import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DatasetNode;
import nl.idgis.publisher.domain.web.tree.DefaultGroupLayer;
import nl.idgis.publisher.domain.web.tree.DefaultService;
import nl.idgis.publisher.domain.web.tree.GroupNode;
import nl.idgis.publisher.domain.web.tree.QDatasetNode;
import nl.idgis.publisher.domain.web.tree.QGroupNode;

import nl.idgis.publisher.service.manager.messages.GetGroupLayer;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.GetServicesWithLayer;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedIterable;
import nl.idgis.publisher.utils.TypedList;
import static com.mysema.query.types.PathMetadataFactory.forVariable;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QDataset.dataset;

public class ServiceManager extends UntypedActor {
	
	private final static QGenericLayer child = new QGenericLayer("child"), parent = new QGenericLayer("parent");
	
	private final static QServiceStructure serviceStructure = new QServiceStructure("service_structure");
	
	static class QServiceStructure extends EntityPathBase<QServiceStructure> {		
		
		private static final long serialVersionUID = -9048925641878000032L;
		
		StringPath serviceIdentification = createString("service_identification");

		NumberPath<Integer> parentLayerId = createNumber("parent_layer_id", Integer.class);
		
		StringPath parentLayerIdentification = createString("parent_layer_identification");
		
		NumberPath<Integer> childLayerId = createNumber("child_layer_id", Integer.class);
		
		StringPath childLayerIdentification = createString("child_layer_identification");
		
		NumberPath<Integer> layerOrder = createNumber("layer_order", Integer.class);
		
		QServiceStructure(String variable) {
	        super(QServiceStructure.class, forVariable(variable));
	        
	        add(serviceIdentification);
	        add(parentLayerId);
	        add(parentLayerIdentification);
	        add(childLayerId);
	        add(childLayerIdentification);
	        add(layerOrder);
	    }
	}
	
	private final static QGroupStructure groupStructure = new QGroupStructure("group_structure");
	
	static class QGroupStructure extends EntityPathBase<QGroupStructure> {		
		
		private static final long serialVersionUID = -9048925641878000032L;
		
		StringPath groupLayerIdentification = createString("group_layer_identification");

		NumberPath<Integer> parentLayerId = createNumber("parent_layer_id", Integer.class);
		
		StringPath parentLayerIdentification = createString("parent_layer_identification");
		
		NumberPath<Integer> childLayerId = createNumber("child_layer_id", Integer.class);
		
		StringPath childLayerIdentification = createString("child_layer_identification");
		
		NumberPath<Integer> layerOrder = createNumber("layer_order", Integer.class);
		
		QGroupStructure(String variable) {
	        super(QGroupStructure.class, forVariable(variable));
	        
	        add(groupLayerIdentification);
	        add(parentLayerId);
	        add(parentLayerIdentification);
	        add(childLayerId);
	        add(childLayerIdentification);
	        add(layerOrder);
	    }
	}
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
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
		} else {
			unhandled(msg);
		}
	}
	
	private void toSender(CompletableFuture<? extends Object> future) {
		ActorRef sender = getSender(), self = getSelf();
		
		future
			.exceptionally(t -> new Failure(t))
			.thenAccept(resp -> sender.tell(resp, self));
	}
	
	private CompletableFuture<TypedIterable<String>> handleGetServicesWithLayer(GetServicesWithLayer msg) {
		String layerId = msg.getLayerId();
		
		return db.transactional(tx ->
			withServiceStructure(tx.query())
				.from(serviceStructure)
				.where(serviceStructure.childLayerIdentification.eq(layerId))
				.distinct()
				.list(serviceStructure.serviceIdentification).thenCompose(child ->
					tx.query().from(service)
						.join(genericLayer).on(genericLayer.id.eq(service.rootgroupId))
						.where(genericLayer.identification.eq(layerId))
						.distinct()						
						.list(service.identification).thenApply(root -> {
							Set<String> serviceIds = new HashSet<>();
							serviceIds.addAll(child.list());
							serviceIds.addAll(root.list());
							
							return new TypedIterable<>(String.class, serviceIds);
						})));
	}

	@SuppressWarnings("unchecked")
	private CompletableFuture<Object> handleGetGroupLayer(GetGroupLayer msg) {
		String groupLayerId = msg.getGroupLayerId();
		
		return db.transactional(tx -> {
			AsyncSQLQuery withGroupStructure = 
			tx.query().withRecursive(groupStructure, 
					groupStructure.groupLayerIdentification,
					groupStructure.childLayerId, 
					groupStructure.childLayerIdentification,
					groupStructure.parentLayerId,
					groupStructure.parentLayerIdentification,
					groupStructure.layerOrder).as(
				new SQLSubQuery().unionAll(
					new SQLSubQuery().from(layerStructure)
						.join(child).on(child.id.eq(layerStructure.childLayerId))
						.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
						.join(genericLayer).on(genericLayer.id.eq(layerStructure.parentLayerId))						
						.list(
							genericLayer.identification, 
							child.id,
							child.identification,
							parent.id,
							parent.identification,
							layerStructure.layerOrder),
					new SQLSubQuery().from(layerStructure)
						.join(child).on(child.id.eq(layerStructure.childLayerId))
						.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
						.join(groupStructure).on(groupStructure.childLayerId.eq(layerStructure.parentLayerId))
						.list(
							groupStructure.groupLayerIdentification, 
							child.id,
							child.identification,
							parent.id,
							parent.identification,
							layerStructure.layerOrder)));
			
			CompletableFuture<TypedList<Tuple>> structure = withGroupStructure.clone()
				.from(groupStructure)
				.where(groupStructure.groupLayerIdentification.eq(groupLayerId))
				.orderBy(groupStructure.layerOrder.asc())
				.list(groupStructure.childLayerIdentification, groupStructure.parentLayerIdentification);
			
			CompletableFuture<Optional<GroupNode>> root = tx.query().from(genericLayer)				
				.where(genericLayer.identification.eq(groupLayerId)
					.and(new SQLSubQuery().from(leafLayer)
						.where(leafLayer.genericLayerId.eq(genericLayer.id))
						.notExists()))
				.singleResult(new QGroupNode(
					genericLayer.identification, 
					genericLayer.name, 
					genericLayer.title, 
					genericLayer.abstractCol));
			
			CompletableFuture<TypedList<GroupNode>> groups = withGroupStructure.clone()
				.from(genericLayer)
				.join(groupStructure).on(groupStructure.childLayerId.eq(genericLayer.id))
				.where(new SQLSubQuery().from(leafLayer)
					.where(leafLayer.genericLayerId.eq(genericLayer.id))
					.notExists())	
				.where(groupStructure.groupLayerIdentification.eq(groupLayerId))
				.list(new QGroupNode(
					genericLayer.identification, 
					genericLayer.name, 
					genericLayer.title, 
					genericLayer.abstractCol));
			
			// last query -> .clone() not required
			CompletableFuture<TypedList<DatasetNode>> datasets = withGroupStructure  
				.from(leafLayer)
				.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
				.join(dataset).on(dataset.id.eq(leafLayer.datasetId))
				.join(groupStructure).on(groupStructure.childLayerId.eq(genericLayer.id))
				.where(groupStructure.groupLayerIdentification.eq(groupLayerId))
				.list(new QDatasetNode(genericLayer.identification, 
					genericLayer.name, 
					genericLayer.title, 
					genericLayer.abstractCol,
					dataset.name));
			
			return root.thenCompose(rootResult ->
				rootResult.isPresent()
				? structure.thenCompose(structureResult ->
					groups.thenCompose(groupsResult ->										
						datasets.thenApply(datasetsResult -> {							
							// LinkedHashMap is used to preserve layer order
							Map<String, String> structureMap = new LinkedHashMap<>();
							
							for(Tuple structureTuple : structureResult) {
								structureMap.put(
									structureTuple.get(groupStructure.childLayerIdentification),
									structureTuple.get(groupStructure.parentLayerIdentification));
							}
			
							return new DefaultGroupLayer(
								rootResult.get(), 
								datasetsResult.list(),
								groupsResult.list(),
								structureMap);
						})))
				: f.successful(new NotFound()));
		});
	}

	@SuppressWarnings("unchecked")
	private CompletableFuture<Object> handleGetService(GetService msg) {
		String serviceId = msg.getServiceId();
		
		return db.transactional(tx -> {
			AsyncSQLQuery withServiceStructure = withServiceStructure(tx.query());
			
			CompletableFuture<TypedList<Tuple>> structure = withServiceStructure.clone()
				.from(serviceStructure)
				.where(serviceStructure.serviceIdentification.eq(serviceId))
				.orderBy(serviceStructure.layerOrder.asc())
				.list(serviceStructure.childLayerIdentification, serviceStructure.parentLayerIdentification);
			
			CompletableFuture<Optional<GroupNode>> root = tx.query().from(genericLayer)
				.join(service).on(service.rootgroupId.eq(genericLayer.id))
				.where(service.identification.eq(serviceId))
				.singleResult(new QGroupNode(
					genericLayer.identification, 
					genericLayer.name, 
					genericLayer.title, 
					genericLayer.abstractCol));
			
			CompletableFuture<TypedList<GroupNode>> groups = withServiceStructure.clone()
				.from(genericLayer)
				.join(serviceStructure).on(serviceStructure.childLayerId.eq(genericLayer.id))
				.where(new SQLSubQuery().from(leafLayer)
					.where(leafLayer.genericLayerId.eq(genericLayer.id))
					.notExists())	
				.where(serviceStructure.serviceIdentification.eq(serviceId))
				.list(new QGroupNode(
					genericLayer.identification, 
					genericLayer.name, 
					genericLayer.title, 
					genericLayer.abstractCol));
			
			// last query -> .clone() not required
			CompletableFuture<TypedList<DatasetNode>> datasets = withServiceStructure  
				.from(leafLayer)
				.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
				.join(dataset).on(dataset.id.eq(leafLayer.datasetId))
				.join(serviceStructure).on(serviceStructure.childLayerId.eq(genericLayer.id))
				.where(serviceStructure.serviceIdentification.eq(serviceId))
				.list(new QDatasetNode(genericLayer.identification, 
					genericLayer.name, 
					genericLayer.title, 
					genericLayer.abstractCol,
					dataset.name));
			
			CompletableFuture<Optional<Tuple>> serviceInfo = tx.query().from(service)
				.where(service.identification.eq(serviceId))
				.singleResult(
					service.name,
					service.title);
			
			return root.thenCompose(rootResult ->
				rootResult.isPresent()
				? structure.thenCompose(structureResult ->
					groups.thenCompose(groupsResult ->	
						serviceInfo.thenCompose(serviceInfoResult -> 
							datasets.thenApply(datasetsResult -> {							
								// LinkedHashMap is used to preserve layer order
								Map<String, String> structureMap = new LinkedHashMap<>();
								
								for(Tuple structureTuple : structureResult) {
									structureMap.put(
										structureTuple.get(serviceStructure.childLayerIdentification),
										structureTuple.get(serviceStructure.parentLayerIdentification));
								}
				
								return new DefaultService(
									serviceId, 
									serviceInfoResult.get().get(service.name),
									rootResult.get(), 
									datasetsResult.list(), 
									groupsResult.list(), 
									structureMap);
							}))))
				: f.successful(new NotFound()));
		});
	}

	@SuppressWarnings("unchecked")
	private <Q extends SQLCommonQuery<Q>> Q withServiceStructure(SQLCommonQuery<Q> query) {
		return query.withRecursive(serviceStructure,
			serviceStructure.serviceIdentification,
			serviceStructure.childLayerId, 
			serviceStructure.childLayerIdentification,
			serviceStructure.parentLayerId,
			serviceStructure.parentLayerIdentification,
			serviceStructure.layerOrder).as(
			new SQLSubQuery().unionAll(
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(service).on(service.rootgroupId.eq(layerStructure.parentLayerId))						
					.list(
						service.identification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder),
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(serviceStructure).on(serviceStructure.childLayerId.eq(layerStructure.parentLayerId))
					.list(
						serviceStructure.serviceIdentification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder)));
	}
}
