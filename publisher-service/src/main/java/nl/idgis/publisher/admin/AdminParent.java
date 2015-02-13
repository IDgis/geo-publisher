package nl.idgis.publisher.admin;

import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.admin.messages.AddDelete;
import nl.idgis.publisher.admin.messages.AddGet;
import nl.idgis.publisher.admin.messages.AddList;
import nl.idgis.publisher.admin.messages.AddPut;
import nl.idgis.publisher.admin.messages.AddQuery;

import nl.idgis.publisher.domain.query.DeleteEntity;
import nl.idgis.publisher.domain.query.DomainQuery;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.PutEntity;

public class AdminParent extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, harvester, loader, service, jobSystem, serviceManager;
	
	private Map<Class<?>, ActorRef> get, list, query, delete, put;
	
	public AdminParent(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, ActorRef jobSystem, ActorRef serviceManager) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
		this.service = service;
		this.jobSystem = jobSystem;
		this.serviceManager = serviceManager;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, ActorRef jobSystem, ActorRef serviceManager) {
		return Props.create(AdminParent.class, database, harvester, loader, service, jobSystem, serviceManager);
	}
	
	@Override
	public void preStart() throws Exception {
		get = new HashMap<>();
		list = new HashMap<>();
		query = new HashMap<>();
		delete = new HashMap<>();
		put = new HashMap<>();
		
		getContext().actorOf(Admin.props(database, harvester, loader, service, jobSystem), "admin");
		getContext().actorOf(DataSourceAdmin.props(database, harvester), "data-source");
		getContext().actorOf(CategoryAdmin.props(database), "category");
		getContext().actorOf(DatasetAdmin.props(database), "dataset");
		getContext().actorOf(ServiceAdmin.props(database, serviceManager), "service");
		getContext().actorOf(LayerAdmin.props(database), "layer");
		getContext().actorOf(LayerGroupAdmin.props(database, serviceManager), "layergroup");
		getContext().actorOf(TiledLayerAdmin.props(database), "tiledlayer");
		getContext().actorOf(StyleAdmin.props(database), "style");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetEntity) {
			Class<?> entity = ((GetEntity<?>)msg).cls();
			
			if(get.containsKey(entity)) {
				log.debug("forwarding get query");
				get.get(entity).forward(msg, getContext());
			} else {
				log.error("Unhandled GetEntity message: {}", entity);				
				unhandled(msg);
			}
		} else if(msg instanceof ListEntity) {
			Class<?> entity = ((ListEntity<?>)msg).cls();
			
			if(list.containsKey(entity)) {
				log.debug("forwarding list query");
				list.get(entity).forward(msg, getContext());
			} else {
				log.error("Unhandled ListEntity message: {}", entity);
				unhandled(msg);
			}
		} else if(msg instanceof DeleteEntity) {
			Class<?> entity = ((DeleteEntity<?>)msg).cls();
			
			if(delete.containsKey(entity)) {
				log.debug("forwarding query");
				delete.get(entity).forward(msg, getContext());
			} else {
				log.error("Unhandled DeleteEntity message: {}", entity);
				unhandled(msg);
			}
		} else if(msg instanceof PutEntity) {
			Class<?> entity = ((PutEntity<?>)msg).value().getClass();
			
			if(put.containsKey(entity)) {
				log.debug("forwarding query");
				put.get(entity).forward(msg, getContext());
			} else {
				log.error("Unhandled PutEntity message: {}", entity);
				unhandled(msg);
			}
		} else if(msg instanceof DomainQuery) {
			Class<?> clazz = msg.getClass();
			
			if(query.containsKey(clazz)) {
				log.debug("forwarding query");
				query.get(clazz).forward(msg, getContext());
			} else {
				log.error("Unhandled DomainQuery message: {}", clazz);
				unhandled(msg);
			}
		} else if(msg instanceof AddGet) {
			Class<?> entity = ((AddGet)msg).getEntity();
			ActorRef sender = getSender();
			
			log.debug("registering get query: {} on actor '{}'", entity, sender.path().name());
			get.put(entity, sender);
		} else if(msg instanceof AddList) {
			Class<?> entity = ((AddList)msg).getEntity();
			ActorRef sender = getSender();
			
			log.debug("registering list query: {} on actor '{}'", entity, sender.path().name());
			list.put(entity, sender);
		} else if(msg instanceof AddQuery) {
			Class<?> clazz = ((AddQuery)msg).getClazz();
			ActorRef sender = getSender();
			
			log.debug("registering query: {} on actor '{}'", clazz, sender.path().name());
			query.put(clazz, sender);
		} else if(msg instanceof AddDelete) {
			Class<?> entity= ((AddDelete)msg).getEntity();
			ActorRef sender = getSender();
			
			log.debug("registering delete query: {} on actor '{}'", entity, sender.path().name());
			delete.put(entity, sender);
		} else if(msg instanceof AddPut) {
			Class<?> entity = ((AddPut)msg).getEntity();
			ActorRef sender = getSender();
			
			log.debug("registering put query: {} on actor '{}'", entity, sender.path().name());
			put.put(entity, sender);
		} else {
			unhandled(msg);
		}
	}
}
