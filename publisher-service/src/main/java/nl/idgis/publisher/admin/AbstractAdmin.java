package nl.idgis.publisher.admin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mysema.query.SimpleQuery;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActorWithStash;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import akka.util.Timeout;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.admin.messages.DoDelete;
import nl.idgis.publisher.admin.messages.DoGet;
import nl.idgis.publisher.admin.messages.DoList;
import nl.idgis.publisher.admin.messages.DoPut;
import nl.idgis.publisher.admin.messages.DoQuery;
import nl.idgis.publisher.admin.messages.DeleteEvent;
import nl.idgis.publisher.admin.messages.Event;
import nl.idgis.publisher.admin.messages.EventCompleted;
import nl.idgis.publisher.admin.messages.EventFailed;
import nl.idgis.publisher.admin.messages.EventWaiting;
import nl.idgis.publisher.admin.messages.Initialized;
import nl.idgis.publisher.admin.messages.OnDelete;
import nl.idgis.publisher.admin.messages.OnPut;
import nl.idgis.publisher.admin.messages.OnQuery;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.domain.Failure;
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

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public abstract class AbstractAdmin extends UntypedActorWithStash {
	
	protected static final long DEFAULT_ITEMS_PER_PAGE = 20;
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final ActorRef database;	
	
	protected FutureUtils f;
	
	protected AsyncDatabaseHelper db;
	
	@SuppressWarnings("rawtypes")
	protected Map<Class, Function> doQuery, doList, doGet, doDelete, doPut, onDeleteBefore;
		
	@SuppressWarnings("rawtypes")
	protected Map<Class, Consumer> onQuery;
	
	@SuppressWarnings("rawtypes")
	protected Map<Class, BiConsumer> onDeleteAfter, onPut;
	
	public AbstractAdmin(ActorRef database) {
		this.database = database;
	}
	
	protected void singlePage(SimpleQuery<?> query, Long page) {
		singlePage(query, page, DEFAULT_ITEMS_PER_PAGE);
	}
	
	protected void singlePage(SimpleQuery<?> query, Long page, Optional<Long> itemsPerPage) {
		if(itemsPerPage.isPresent()) {
			singlePage(query, page, itemsPerPage.get());
		} else {
			singlePage(query, page);
		}
	}
	
	protected void singlePage(SimpleQuery<?> query, Long page, long itemsPerPage) {
		if(page == null) {
			return;
		}
		
		if(page > 0) {
			query.offset((page - 1) * itemsPerPage);
			query.limit(itemsPerPage);
		} else {
			throw new IllegalArgumentException("page parameter should be > 0");
		}
	}
	
	protected void addPageInfo(Page.Builder<?> pageBuilder, Long page, long count) {
		addPageInfo(pageBuilder, page, count, DEFAULT_ITEMS_PER_PAGE);
	}
	
	protected void addPageInfo(Page.Builder<?> pageBuilder, Long page, long count, Optional<Long> itemsPerPage) {
		if(itemsPerPage.isPresent()) {
			addPageInfo(pageBuilder, page, count, itemsPerPage.get());
		} else {
			addPageInfo(pageBuilder, page, count);
		}
	}
	
	protected void addPageInfo(Page.Builder<?> pageBuilder, Long page, long count, long itemsPerPage) {
		log.debug("adding page info, page: {}, count: {}", page, count);
		
		if(page != null) {
			long pages = count / itemsPerPage + Math.min(1, count % itemsPerPage);
			
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
	
	protected <T, U extends DomainQuery<? super T>> void doQueryOptional(Class<U> query, Function<U, CompletableFuture<Optional<T>>> func) {	
		doQuery.put(query, func);
		getContext().parent().tell(new DoQuery(query), getSelf());
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
	
	protected <T extends Identifiable> void onDelete(Class<? super T> entity, Runnable after) {
		this.<T, Void>onDelete(entity, id -> f.successful(null), (v, responseValue) -> after.run());
	}
	
	protected <T extends Identifiable> void onDelete(Class<? super T> entity, Consumer<String> after) {
		this.<T, Void>onDelete(entity, id -> f.successful(null), (v, responseValue) -> after.accept(responseValue));
	}
	
	protected <T extends Identifiable, U> void onDelete(Class<? super T> entity, Function<String, CompletableFuture<U>> before, BiConsumer<U, String> after) {
		onDeleteBefore.put(entity, before);
		onDeleteAfter.put(entity, after);
		getContext().parent().tell(new OnDelete(entity), getSelf());
	}
	
	protected <T extends Identifiable> void doPut(Class<? super T> entity, Function<T, CompletableFuture<Response<?>>> func) {
		doPut.put(entity, func);
		getContext().parent().tell(new DoPut(entity), getSelf());
	}
	
	protected <T extends Identifiable> void onPut(Class<? super T> entity, BiConsumer<T, String> func) {
		onPut.put(entity, func);
		getContext().parent().tell(new OnPut(entity), getSelf());
	}
	
	protected abstract void preStartAdmin();
	
	@Override
	public final void preStart() throws Exception {
		f = new FutureUtils(getContext(), Timeout.apply(15000));		
		db = new AsyncDatabaseHelper(database, f, log);
		
		doQuery = new HashMap<>();
		doList = new HashMap<>();
		doGet = new HashMap<>();
		doDelete = new HashMap<>();
		doPut = new HashMap<>();
		
		onQuery = new HashMap<>();
		onDeleteBefore = new HashMap<>();
		onDeleteAfter = new HashMap<>();
		onPut = new HashMap<>();
		
		preStartAdmin();
		
		getContext().parent().tell(new Initialized(), getSelf());
	}
	
	private <T> void toSender(CompletableFuture<T> future) throws Exception {
		ActorRef sender = getSender();
		future.whenComplete((resp, t) -> {
			if(t != null) {
				log.debug("future completed exceptionally");
				failure(t, sender);
			} else {
				try {
					Object result;
					if(resp instanceof Optional) {
						Optional<?> opt = ((Optional<?>)resp);
						if(opt.isPresent()) {
							result = opt.get();
						} else {
							result = new NotFound();
						}
					} else {
						result = resp;
					}
					
					log.debug("sending response: {} to {}", resp, sender);
					sender.tell(result, getSelf());
				} catch(Exception e) {
					log.debug("exception while sending response");
					failure(e, sender);
				}
			}
		});
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof Event) {
			handleEvent((Event)msg);
		} else if(msg instanceof DeleteEvent) {
			handleDeleteEvent((DeleteEvent)msg);
		} else if(msg instanceof DomainQuery) {
			try {
				if(msg instanceof GetEntity) {
					handleGetEntity((GetEntity<?>)msg);
				} else if(msg instanceof ListEntity) {
					handleListEntity((ListEntity<?>)msg);
				} else if(msg instanceof DeleteEntity) {
					handleDeleteEntity((DeleteEntity<?>)msg);
				} else if(msg instanceof PutEntity) {
					handlePutEntity((PutEntity<?>)msg);
				} else {			
					handleDomainQuery((DomainQuery<?>)msg);
				}
			} catch(Exception e) {
				log.debug("exception during query handling");
				failure(e, getSender());
			}
		} else {
			unhandled(msg);
		}
	}

	private void handleDeleteEvent(DeleteEvent msg) {
		Class<?> entity = msg.getMessage().cls();
		
		@SuppressWarnings("unchecked")
		Function<String, CompletableFuture<Object>> beforeHandler = onDeleteBefore.get(entity);
		if(beforeHandler == null) {
			log.debug("delete event not handled: {}", entity);
			
			unhandled(msg);
		} else {
			log.debug("handling delete event: {} from {}", entity, getSender());
			
			beforeHandler.apply(msg.getMessage().id()).whenComplete((result, t) -> {
				if(t != null) {
					result = t;
				}
				
				getSelf().tell(new BeforeCompleted(result), getSelf());
			});
			
			getContext().setReceiveTimeout(Duration.create(10, TimeUnit.SECONDS));
			getContext().become(beforeDelete(getSender(), entity));
		}
	}

	private void handleDomainQuery(DomainQuery<?> msg) throws Exception {
		Class<?> clazz = msg.getClass();
		
		@SuppressWarnings("unchecked")
		Function<DomainQuery<?>, CompletableFuture<?>> handler = doQuery.get(clazz);
		if(handler == null) {
			log.debug("query not handled: {}", msg);
			
			unhandled(msg);
		} else {
			log.debug("handling query: {}", msg);
			
			toSender(handler.apply(msg));
		}
	}

	private void handlePutEntity(PutEntity<?> msg) throws Exception {
		Object value = msg.value();
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
	}

	private void handleDeleteEntity(DeleteEntity<?> msg) throws Exception {
		Class<?> entity = msg.cls();
		
		@SuppressWarnings("unchecked")
		Function<String, CompletableFuture<?>> handler = doDelete.get(entity);
		if(handler == null) {
			log.debug("delete not handled: {}", entity);
			
			unhandled(msg);
		} else {
			log.debug("handling delete: {}", entity);
			
			toSender(handler.apply(msg.id()));
		}
	}

	private void handleListEntity(ListEntity<?> msg) throws Exception {
		Class<?> entity = msg.cls();
		
		@SuppressWarnings("unchecked")
		Function<Long, CompletableFuture<?>> handler = doList.get(entity);
		if(handler == null) {
			log.debug("list entity not handled: {}", entity);
			
			unhandled(msg);
		} else {
			log.debug("handling list entity: {}", entity);
			
			toSender(handler.apply(msg.page()));
		}
	}

	private void handleGetEntity(GetEntity<?> msg) throws Exception {
		Class<?> entity = msg.cls();
		
		@SuppressWarnings("unchecked")
		Function<String, CompletableFuture<Optional<Object>>> handler = doGet.get(entity);
		if(handler == null) {
			log.debug("get entity not handled: {}", entity);
			
			unhandled(msg);
		} else {
			log.debug("handling get entity: {}", entity);
			
			toSender(handler.apply(msg.id()));
		}
	}
	
	private static class BeforeCompleted {
		
		private final Object result;
		
		BeforeCompleted(Object result) {
			this.result = result;
		}
		
		Object getResult() {
			return result;
		}
	}
	
	private Procedure<Object> afterDelete(ActorRef sender, Class<?> entity, Object beforeResult) {
		return new Procedure<Object>() {
			
			@Override
			@SuppressWarnings("unchecked")
			public void apply(Object msg) throws Exception {
				if(msg instanceof ReceiveTimeout) {
					log.error("timeout while waiting for event to complete");
					
					finish();
				} else if(msg instanceof EventCompleted && getSender().equals(sender)) {
					log.debug("event completed");
					
					onDeleteAfter.get(entity).accept(beforeResult, ((EventCompleted<?>)msg).getValue());
					
					finish();
				} else if(msg instanceof EventFailed && getSender().equals(sender)) {
					log.debug("event failed");
					
					finish();
				} else {
					log.debug("message stashed: {} from {}", msg, getSender());
					
					stash();
				}
			}
			
			private void finish() {
				log.debug("delete event finished");
				
				unstashAll();
				getContext().setReceiveTimeout(Duration.Inf());
				getContext().become(receive());
			}
			
		};
	}
	
	private Procedure<Object> beforeDelete(ActorRef sender, Class<?> entity) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof ReceiveTimeout) {
					log.error("timeout while waiting for beforeDelete to complete");
					
					sender.tell(new EventFailed(), getSelf());
					getContext().setReceiveTimeout(Duration.Inf());
					getContext().become(receive());
				} else if(msg instanceof BeforeCompleted) {
					log.debug("before completed");
					
					sender.tell(new EventWaiting(), getSelf());
					getContext().become(afterDelete(sender, entity, ((BeforeCompleted)msg).getResult()));
				} else {
					log.debug("message stashed: {} from {}", msg, getSender());
					
					stash();
				}
			}
			
		};
	}

	private void handleEvent(Event msg) throws Exception {
		log.debug("event received: {}", msg);
		
		Object request = msg.getRequest();
		if(request instanceof PutEntity) {
			Object value = ((PutEntity<?>)request).value();
			Class<?> entity = value.getClass();
			
			@SuppressWarnings("unchecked")
			BiConsumer<Object, String> handler = onPut.get(entity);
			if(handler == null) {
				log.debug("put event not handled: {}", entity);
				
				unhandled(msg);
			} else {
				log.debug("handling put: {}", entity);
				
				handler.accept(value, (String)((Response<?>)msg.getResponse()).getValue());
			}
		} else if(request instanceof DomainQuery) {
			Class<?> clazz = request.getClass();
			
			@SuppressWarnings("unchecked")
			Consumer<DomainQuery<?>> handler = onQuery.get(clazz);
			if(handler == null) {
				log.debug("query not handled: {}", request);
				
				unhandled(msg);
			} else {
				log.debug("handling query: {}", request);
				
				handler.accept((DomainQuery<?>)request);
			}
		} else {
			log.error("unhandled event: {}", msg);
			
			unhandled(msg);
		}
	}
	
	private void failure(Throwable t, ActorRef sender) {
		log.debug("sending failure: {} {}", t, sender);
		
		try {
			String exceptionId = UUID.randomUUID().toString();
			
			StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			
			log.error("failure: {} {}", exceptionId, sw.toString());
			sender.tell(new Failure(exceptionId), getSelf());
		} catch(Exception e) {
			log.error("sending failure failed: {}", e);
		}
	}

}
