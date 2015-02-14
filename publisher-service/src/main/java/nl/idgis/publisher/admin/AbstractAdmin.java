package nl.idgis.publisher.admin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mysema.query.SimpleQuery;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import nl.idgis.publisher.admin.messages.DoDelete;
import nl.idgis.publisher.admin.messages.DoGet;
import nl.idgis.publisher.admin.messages.DoList;
import nl.idgis.publisher.admin.messages.DoPut;
import nl.idgis.publisher.admin.messages.DoQuery;
import nl.idgis.publisher.admin.messages.OnDelete;
import nl.idgis.publisher.admin.messages.OnPut;
import nl.idgis.publisher.admin.messages.OnQuery;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.domain.query.DeleteEntity;
import nl.idgis.publisher.domain.query.DomainQuery;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.PutEntity;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.web.Entity;
import nl.idgis.publisher.domain.web.Identifiable;
import nl.idgis.publisher.domain.web.NotFound;

import nl.idgis.publisher.utils.Event;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public abstract class AbstractAdmin extends UntypedActor {
	
	protected static final long ITEMS_PER_PAGE = 20;
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final ActorRef database;	
	
	protected FutureUtils f;
	
	protected AsyncDatabaseHelper db;
	
	@SuppressWarnings("rawtypes")
	protected Map<Class, Function> doQuery, doList, doGet, doDelete, doPut;
		
	@SuppressWarnings("rawtypes")
	protected Map<Class, Consumer> onQuery, onDelete, onPut; 
	
	public AbstractAdmin(ActorRef database) {
		this.database = database;
	}
	
	protected void singlePage(SimpleQuery<?> query, Long page) {
		if(page == null) {
			return;
		}
		
		if(page > 0) {
			query.offset((page - 1) * ITEMS_PER_PAGE);
			query.limit(ITEMS_PER_PAGE);
		} else {
			throw new IllegalArgumentException("page parameter should be > 0");
		}
	}
	
	protected void addPageInfo(Page.Builder<?> pageBuilder, Long page, long count) {
		if(page != null) {
			long pages = count / ITEMS_PER_PAGE + Math.min(1, count % ITEMS_PER_PAGE);
			
			if(pages > 1) {
				pageBuilder
					.setHasMorePages(true)
					.setPageCount(pages)
					.setCurrentPage(page);
			}
		}
	}
	
	protected <T extends Entity> Page<T> toPage(TypedList<T> list) {
		Page.Builder<T> pageBuilder = new Page.Builder<>();
		pageBuilder.addAll(list.asCollection());
		return pageBuilder.build();
	}
	
	protected <T extends Entity> List<T> toList(TypedList<T> list) {
		return list.list();
	}
	
	protected <T, U extends DomainQuery<? super T>> void doQuery(Class<U> query, Function<U, CompletableFuture<T>> func) {	
		doQuery.put(query, func);
		getContext().parent().tell(new DoQuery(query), getSelf());
	}
	
	protected <T, U extends DomainQuery<? super T>> void onQuery(Class<U> query, Consumer<U> func) {	
		onQuery.put(query, func);
		getContext().parent().tell(new OnQuery(query), getSelf());
	}
	
	protected <T extends Entity> void doList(Class<? super T> entity, Supplier<CompletableFuture<Page<T>>> func) {
		doList(entity, (Long page) -> func.get());
	}
	
	protected <T extends Entity> void doList(Class<? super T> entity, Function<Long, CompletableFuture<Page<T>>> func) {	
		doList.put(entity, func);
		getContext().parent().tell(new DoList(entity), getSelf());
	}
	
	protected <T extends Entity> void doGet(Class<? super T> entity, Function<String, CompletableFuture<Optional<T>>> func) {	
		doGet.put(entity, func);
		getContext().parent().tell(new DoGet(entity), getSelf());
	}
	
	protected <T extends Identifiable> void doDelete(Class<? super T> entity, Function<String, CompletableFuture<Response<?>>> func) {
		doDelete.put(entity, func);
		getContext().parent().tell(new DoDelete(entity), getSelf());
	}
	
	protected <T extends Identifiable> void onDelete(Class<? super T> entity, Consumer<String> func) {
		onDelete.put(entity, func);
		getContext().parent().tell(new OnDelete(entity), getSelf());
	}
	
	protected <T extends Identifiable> void doPut(Class<? super T> entity, Function<T, CompletableFuture<Response<?>>> func) {
		doPut.put(entity, func);
		getContext().parent().tell(new DoPut(entity), getSelf());
	}
	
	protected <T extends Identifiable> void onPut(Class<? super T> entity, Consumer<T> func) {
		onPut.put(entity, func);
		getContext().parent().tell(new OnPut(entity), getSelf());
	}
	
	protected abstract void preStartAdmin();
	
	@Override
	public final void preStart() throws Exception {
		f = new FutureUtils(getContext().dispatcher(), Timeout.apply(15000));		
		db = new AsyncDatabaseHelper(database, f, log);
		
		doQuery = new HashMap<>();
		doList = new HashMap<>();
		doGet = new HashMap<>();
		doDelete = new HashMap<>();
		doPut = new HashMap<>();
		
		onQuery = new HashMap<>();
		onDelete = new HashMap<>();
		onPut = new HashMap<>();
		
		preStartAdmin();
	}
	
	private void toSender(CompletableFuture<?> future) throws Exception {
		ActorRef sender = getSender(), self = getSelf();		
		future.whenComplete((resp, t) -> {
			if(t != null) {
				StringWriter sw = new StringWriter();
				t.printStackTrace(new PrintWriter(sw));
				log.error("failure: {}", sw);
			} else {
				log.debug("sending response: {}", resp);				
				sender.tell(resp, self);
			}
		});
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof Event) {
			handleEvent((Event)msg);
		} else if(msg instanceof GetEntity) {
			Class<?> entity = ((GetEntity<?>)msg).cls();
			
			@SuppressWarnings("unchecked")
			Function<String, CompletableFuture<Optional<Object>>> handler = doGet.get(entity);
			if(handler == null) {
				log.debug("get entity not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling get entity: {}", entity);
				
				toSender(handler.apply(((GetEntity<?>)msg).id())
					.thenApply(resp -> resp.orElse(new NotFound())));
			}
		} else if(msg instanceof ListEntity) {
			Class<?> entity = ((ListEntity<?>)msg).cls();
			
			@SuppressWarnings("unchecked")
			Function<Long, CompletableFuture<?>> handler = doList.get(entity);
			if(handler == null) {
				log.debug("list entity not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling list entity: {}", entity);
				
				toSender(handler.apply(((ListEntity<?>)msg).page()));
			}
		} else if(msg instanceof DeleteEntity) {
			Class<?> entity = ((DeleteEntity<?>)msg).cls();
			
			@SuppressWarnings("unchecked")
			Function<String, CompletableFuture<?>> handler = doDelete.get(entity);
			if(handler == null) {
				log.debug("delete not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling delete: {}", entity);
				
				toSender(handler.apply(((DeleteEntity<?>)msg).id()));
			}
		} else if(msg instanceof PutEntity) {
			Object value = ((PutEntity<?>)msg).value();
			Class<?> entity = value.getClass();
			
			@SuppressWarnings("unchecked")
			Function<Object, CompletableFuture<?>> handler = doPut.get(entity);
			if(handler == null) {
				log.debug("put not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling put: {}", entity);
				
				toSender(handler.apply(value));
			}
		} else if(msg instanceof DomainQuery) {
			Class<?> clazz = msg.getClass();
			
			@SuppressWarnings("unchecked")
			Function<DomainQuery<?>, CompletableFuture<?>> handler = doQuery.get(clazz);
			if(handler == null) {
				log.debug("query not handled: {}", msg);
				
				unhandled(msg);
			} else {
				log.debug("handling query: {}", msg);
				
				toSender(handler.apply((DomainQuery<?>)msg));
			}
		} else {
			unhandled(msg);
		}
	}

	private void handleEvent(Event msg) throws Exception {
		log.debug("event received: {}", msg);
		
		Object eventMsg = ((Event)msg).getMessage();
		if(eventMsg instanceof DeleteEntity) {
			Class<?> entity = ((DeleteEntity<?>)eventMsg).cls();
			
			@SuppressWarnings("unchecked")
			Consumer<String> handler = onDelete.get(entity);
			if(handler == null) {
				log.debug("delete event not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling delete event: {}", entity);
				
				handler.accept(((DeleteEntity<?>)eventMsg).id());
			}
		} else if(eventMsg instanceof PutEntity) {
			Object value = ((PutEntity<?>)eventMsg).value();
			Class<?> entity = value.getClass();
			
			@SuppressWarnings("unchecked")
			Consumer<Object> handler = onPut.get(entity);
			if(handler == null) {
				log.debug("put event not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling put: {}", entity);
				
				handler.accept(value);
			}
		} else if(eventMsg instanceof DomainQuery) {
			Class<?> clazz = eventMsg.getClass();
			
			@SuppressWarnings("unchecked")
			Consumer<DomainQuery<?>> handler = onQuery.get(clazz);
			if(handler == null) {
				log.debug("query not handled: {}", eventMsg);
				
				unhandled(msg);
			} else {
				log.debug("handling query: {}", eventMsg);
				
				handler.accept((DomainQuery<?>)eventMsg);
			}
		} else {
			log.error("unhandled event: {}", msg);
			
			unhandled(msg);
		}
	}

}
