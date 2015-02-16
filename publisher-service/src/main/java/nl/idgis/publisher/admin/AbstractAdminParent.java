package nl.idgis.publisher.admin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorWithStash;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.admin.messages.DoDelete;
import nl.idgis.publisher.admin.messages.DoGet;
import nl.idgis.publisher.admin.messages.DoList;
import nl.idgis.publisher.admin.messages.DoPut;
import nl.idgis.publisher.admin.messages.DoQuery;
import nl.idgis.publisher.admin.messages.Event;
import nl.idgis.publisher.admin.messages.Initialized;
import nl.idgis.publisher.admin.messages.OnDelete;
import nl.idgis.publisher.admin.messages.OnPut;
import nl.idgis.publisher.admin.messages.OnQuery;

import nl.idgis.publisher.domain.query.DeleteEntity;
import nl.idgis.publisher.domain.query.DomainQuery;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.PutEntity;

import nl.idgis.publisher.utils.UniqueNameGenerator;

public abstract class AbstractAdminParent extends UntypedActorWithStash {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private Set<ActorRef> initializing;
	
	private Map<Class<?>, ActorRef> doGet, doList, doQuery, doDelete, doPut;
	
	private Map<Class<?>, Set<ActorRef>> onQuery, onDelete, onPut;
	
	@Override
	public final void preStart() throws Exception {
		doGet = new HashMap<>();
		doList = new HashMap<>();
		doQuery = new HashMap<>();
		doDelete = new HashMap<>();
		doPut = new HashMap<>();
		onQuery = new HashMap<>();
		onDelete = new HashMap<>();
		onPut = new HashMap<>();
		
		initializing = new HashSet<>();
		
		createActors();
		
		if(initializing.isEmpty()) {
			getContext().become(dispatching());
		}
	}
	
	protected void createAdminActor(Props props, String name) {
		log.debug("creating admin actor: {}", name);
		initializing.add(getContext().actorOf(props, name));
	}
	
	protected abstract void createActors();
	
	private Procedure<Object> dispatching() {
		log.debug("message dispatching started");
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof GetEntity) {
					Class<?> entity = ((GetEntity<?>)msg).cls();
					
					if(doGet.containsKey(entity)) {
						log.debug("forwarding get query");
						doGet.get(entity).forward(msg, getContext());
					} else {
						log.error("Unhandled GetEntity message: {}", entity);				
						unhandled(msg);
					}
				} else if(msg instanceof ListEntity) {
					Class<?> entity = ((ListEntity<?>)msg).cls();
					
					if(doList.containsKey(entity)) {
						log.debug("forwarding list query");
						doList.get(entity).forward(msg, getContext());
					} else {
						log.error("Unhandled ListEntity message: {}", entity);
						unhandled(msg);
					}
				} else if(msg instanceof DeleteEntity) {
					Class<?> entity = ((DeleteEntity<?>)msg).cls();
					
					if(doDelete.containsKey(entity)) {
						log.debug("forwarding query");
						
						if(onDelete.containsKey(entity)) {
							log.debug("creating delete event dispatcher");
							
							Set<ActorRef> listeners = onDelete.get(entity);
							
							ActorRef dispatcher = getContext().actorOf(
								DeleteEventDispatcher.props(msg, getSender(), doDelete.get(entity), listeners),
								nameGenerator.getName(DeleteEventDispatcher.class));
							
							for(ActorRef listener : listeners) {
								listener.tell(new Event(msg), dispatcher);
							}
						} else {
							doDelete.get(entity).tell(msg, getSender());
						}
						
						
					} else {
						log.error("Unhandled DeleteEntity message: {}", entity);
						unhandled(msg);
					}
				} else if(msg instanceof PutEntity) {
					Class<?> entity = ((PutEntity<?>)msg).value().getClass();
					
					if(doPut.containsKey(entity)) {
						log.debug("forwarding query");
						
						ActorRef sender;
						if(onPut.containsKey(entity)) {
							log.debug("creating event dispatcher");
							
							sender = getContext().actorOf(
								EventDispatcher.props(msg, getSender(), onPut.get(entity)),
								nameGenerator.getName(EventDispatcher.class));	
						} else {
							sender = getSender();
						}
						
						doPut.get(entity).tell(msg, sender);
					} else {
						log.error("Unhandled PutEntity message: {}", entity);
						unhandled(msg);
					}
				} else if(msg instanceof DomainQuery) {
					Class<?> clazz = msg.getClass();
					
					if(doQuery.containsKey(clazz)) {
						log.debug("forwarding query");
						
						ActorRef sender;
						if(onQuery.containsKey(clazz)) {
							log.debug("creating event dispatcher");
							
							sender = getContext().actorOf(
								EventDispatcher.props(msg, getSender(), onQuery.get(clazz)),
								nameGenerator.getName(EventDispatcher.class));	
						} else {
							sender = getSender();
						}
						
						doQuery.get(clazz).tell(msg, sender);
					} else {
						log.error("Unhandled DomainQuery message: {}", clazz);
						unhandled(msg);
					}
				} else {
					unhandled(msg);
				}
			}			
		};
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Initialized) {
			log.debug("actor initialized");
			
			initializing.remove(getSender());
			
			if(initializing.isEmpty()) {
				unstashAll();
				getContext().become(dispatching());
			}
		} else if(msg instanceof DoGet) {
			Class<?> entity = ((DoGet)msg).getEntity();
			ActorRef sender = getSender();
			
			log.debug("registering get query: {} on actor '{}'", entity, sender.path().name());
			doGet.put(entity, sender);
		} else if(msg instanceof DoList) {
			Class<?> entity = ((DoList)msg).getEntity();
			ActorRef sender = getSender();
			
			log.debug("registering list query: {} on actor '{}'", entity, sender.path().name());
			doList.put(entity, sender);
		} else if(msg instanceof DoQuery) {
			Class<?> clazz = ((DoQuery)msg).getClazz();
			ActorRef sender = getSender();
			
			log.debug("registering query: {} on actor '{}'", clazz, sender.path().name());
			doQuery.put(clazz, sender);
		} else if(msg instanceof DoDelete) {
			Class<?> entity= ((DoDelete)msg).getEntity();
			ActorRef sender = getSender();
			
			log.debug("registering delete query: {} on actor '{}'", entity, sender.path().name());
			doDelete.put(entity, sender);
		} else if(msg instanceof DoPut) {
			Class<?> entity = ((DoPut)msg).getEntity();
			ActorRef sender = getSender();
			
			log.debug("registering put query: {} on actor '{}'", entity, sender.path().name());
			doPut.put(entity, sender);
		} else if(msg instanceof OnQuery) {
			Class<?> clazz = ((OnQuery)msg).getClazz();
			ActorRef sender = getSender();
			
			log.debug("registering query event: {} on actor '{}'", clazz, sender.path().name());
			
			Set<ActorRef> refs;
			if(onQuery.containsKey(clazz)) {
				refs = onQuery.get(clazz);
			} else {
				refs = new HashSet<>();
				onQuery.put(clazz, refs);
			}
			
			refs.add(sender);
		} else if(msg instanceof OnPut) {
			Class<?> entity = ((OnPut)msg).getEntity();
			ActorRef sender = getSender();
			
			log.debug("registering put query event: {} on actor '{}'", entity, sender.path().name());
			
			Set<ActorRef> refs;
			if(onPut.containsKey(entity)) {
				refs = onPut.get(entity);
			} else {
				refs = new HashSet<>();
				onPut.put(entity, refs);
			}
			
			refs.add(sender);
		} else if(msg instanceof OnDelete) {
			Class<?> entity= ((OnDelete)msg).getEntity();
			ActorRef sender = getSender();
			
			log.debug("registering delete query event: {} on actor '{}'", entity, sender.path().name());
			
			Set<ActorRef> refs;
			if(onDelete.containsKey(entity)) {
				refs = onDelete.get(entity);
			} else {
				refs = new HashSet<>();
				onDelete.put(entity, refs);
			}
			
			refs.add(sender);
		} else {
			log.debug("message stashed: {}", msg);
			
			stash();
		}
	}
}
