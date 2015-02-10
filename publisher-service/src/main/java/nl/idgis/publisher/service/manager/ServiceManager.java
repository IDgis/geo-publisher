package nl.idgis.publisher.service.manager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.Tuple;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.QGenericLayer;

import nl.idgis.publisher.service.manager.messages.DatasetNode;
import nl.idgis.publisher.service.manager.messages.DefaultService;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.GroupNode;
import nl.idgis.publisher.utils.FutureUtils;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QDataSource.dataSource;

public class ServiceManager extends UntypedActor {
	
	private final static QGenericLayer child = new QGenericLayer("child"), parent = new QGenericLayer("parent");
	
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

	private void handleGetService(GetService msg) {
		ActorRef sender = getSender(), self = getSelf();
		
		String serviceId = msg.getServiceId();
		
		db.transactional(tx -> {
			AsyncSQLQuery structureQuery = tx.query().from(layerStructure)
				.join(child).on(child.id.eq(layerStructure.childLayerId))
				.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
				.where(new SQLSubQuery().from(service)
					.where(service.identification.eq(serviceId)
						.and(service.rootgroupId.eq(layerStructure.parentLayerId)))
					.exists());
				
			return structureQuery.clone()
				.orderBy(layerStructure.layerorder.asc())
				.list(child.identification, parent.identification)
				.thenApply(structureResult -> {
					Map<String, String> structure = new LinkedHashMap<>();
					
					for(Tuple structureTuple : structureResult) {
						structure.put(
							structureTuple.get(child.identification),
							structureTuple.get(parent.identification));
					}
					
					tx.query().from(genericLayer)
						.where(genericLayer.id.in(new SQLSubQuery().from(service)
							.where(service.identification.eq(serviceId))
							.list(service.rootgroupId)));
					
					GroupNode root = new GroupNode("group0", "name0", "title0", "abstract0");
					return new DefaultService("service0", root, Collections.<DatasetNode>emptyList(), Collections.singletonList(root), structure);
				});
		}).thenAccept(resp -> sender.tell(resp, self));
		
	}
}
