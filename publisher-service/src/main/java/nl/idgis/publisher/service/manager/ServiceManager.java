package nl.idgis.publisher.service.manager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DatasetNode;
import nl.idgis.publisher.domain.web.tree.DefaultService;
import nl.idgis.publisher.domain.web.tree.GroupNode;
import nl.idgis.publisher.domain.web.tree.QDatasetNode;
import nl.idgis.publisher.domain.web.tree.QGroupNode;

import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.utils.FutureUtils;
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
		
		NumberPath<Integer> layerorder = createNumber("layerorder", Integer.class);
		
		QServiceStructure(String variable) {
	        super(QServiceStructure.class, forVariable(variable));
	        
	        add(serviceIdentification);
	        add(parentLayerId);
	        add(parentLayerIdentification);
	        add(childLayerId);
	        add(childLayerIdentification);
	        add(layerorder);
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
			handleGetService((GetService)msg);
		} else {
			unhandled(msg);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleGetService(GetService msg) {
		ActorRef sender = getSender(), self = getSelf();
		
		String serviceId = msg.getServiceId();
		
		db.transactional(tx -> {
			AsyncSQLQuery withServiceStructure = 
			tx.query().withRecursive(serviceStructure, 
					serviceStructure.serviceIdentification,
					serviceStructure.childLayerId, 
					serviceStructure.childLayerIdentification,
					serviceStructure.parentLayerId,
					serviceStructure.parentLayerIdentification,
					serviceStructure.layerorder).as(
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
			
			CompletableFuture<TypedList<Tuple>> structure = withServiceStructure.clone()
				.from(serviceStructure)
				.where(serviceStructure.serviceIdentification.eq(serviceId))
				.orderBy(serviceStructure.layerorder.asc())
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
			
			return root.thenCompose(rootResult ->
				rootResult.isPresent()
				? structure.thenCompose(structureResult ->
					groups.thenCompose(groupsResult ->										
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
								rootResult.get(), 
								datasetsResult.list(), 
								groupsResult.list(), 
								structureMap);
						})))
				: f.successful(new NotFound()));
		}).thenAccept(resp -> sender.tell(resp, self));
	}
}
